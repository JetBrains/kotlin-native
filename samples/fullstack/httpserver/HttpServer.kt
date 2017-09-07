import jansson.*
import konan.initRuntimeIfNeeded
import kotlinx.cinterop.*
import microhttpd.*
import sqlite3.*

fun makeJson(url: String): String {
    val root = json_object()
    json_object_set_new(root, "url", json_string(url))
    return json_dumps(root, JSON_ENCODE_ANY.toLong())!!.toKString()
}

fun makeHtml(url: String) =
        "<html><head>"+
        "<title>Kotlin</title></head>" +
        "<body>Hello from Kotlin/Native<br/>" +
            "You used <b>$url</b>"+
        "</body></html>"

fun makeResponse(method: String, url: String): Pair<String, String> {
    if (url.startsWith("/json"))
        return "application/json" to makeJson(url)

    return "text/html" to makeHtml(url)
}

fun initHandler(connection: CPointer<MHD_Connection>?) {
    konan.initRuntimeIfNeeded()
    // TODO: read session using cookie and DB.
}

val dbName = "/tmp/clients.dblite"
// `rowid` column is always there in sqlite.
val createDbCommand = """
    CREATE TABLE IF NOT EXISTS clients(
        name VARCHAR(255) NOT NULL
    );
    CREATE TABLE IF NOT EXISTS sessions(
        client INT NOT NULL
    );
"""

typealias CharStarStar = CPointer<CPointerVar<ByteVar>>
typealias DbConnection = CPointerVar<sqlite3>

fun dbOpen(): DbConnection {
    val db = nativeHeap.alloc<DbConnection>()
    if (sqlite3_open(dbName, db.ptr) != 0) {
        throw Error("Cannot open db: ${sqlite3_errmsg(db.value)}")
    }
    return db
}

fun dbExecute(db: DbConnection, command: String,
              callback: Function2<Int, CharStarStar, CharStarStar>? = null) {
    memScoped {
        val error = this.alloc<CPointerVar<ByteVar>>()
        try {
            if (sqlite3_exec(db.value, command, null, null, error.ptr) != 0)
                throw Error("DB error: ${error.value!!.toKString()}")
        } finally {
            sqlite3_free(error.value)
        }
    }
}

fun main(args: Array<String>) {
    if (args.size != 1 || args[0].toIntOrNull() == null) {
        println("HttpServer <port>")
        _exit(1)
    }
    val port = args[0].toInt().toShort()
    val db = dbOpen()
    dbExecute(db, createDbCommand)
    val daemon = MHD_start_daemon(MHD_USE_AUTO or MHD_USE_INTERNAL_POLLING_THREAD or MHD_USE_ERROR_LOG,
        port, null, null, staticCFunction {
            cls, connection, urlC, methodC, _, _, _, ptr -> Int
            // This handler could (and will) be invoked in another per-connection
            // thread.
            val state = initHandler(connection)
            val url = urlC!!.toKString()
            val method = methodC!!.toKString()
            println("Connection to $url method $method")
            if (method != "GET") return@staticCFunction MHD_NO
            val (contentType, response) = makeResponse(method, url)
            return@staticCFunction memScoped {
                val responseC = response.cstr
                val response = MHD_create_response_from_buffer(
                        responseC.size.toLong() - 1, responseC.getPointer(this),
                        MHD_ResponseMemoryMode.MHD_RESPMEM_MUST_COPY)
                MHD_add_response_header (response, "Content-Type", contentType)
                val result = MHD_queue_response(connection, MHD_HTTP_OK, response)
                MHD_destroy_response(response);
                result
            }
        }, null,
        MHD_OPTION_CONNECTION_TIMEOUT, 120,
        MHD_OPTION_STRICT_FOR_CLIENT, 1,
        MHD_OPTION_END)
    if (daemon == null) {
        println("Cannot start daemon")
        _exit(2)
    }
    println("Server started, connect to http://localhost:$port, press Enter to exit...")
    readLine()
    MHD_stop_daemon (daemon)
}