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

val dbName = "/tmp/clients.dblite"
// `rowid` column is always there in sqlite, so no need to create explicit
// primary key.
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

fun fromCArray(ptr: CharStarStar, count: Int): Array<String> {
    val result = Array<String>(count, {
        index -> (ptr+index)!!.pointed.value!!.toKString()
    })
    return result
}

fun dbExecute(db: DbConnection,
              command: String, callback: ((Array<String>, Array<String>)-> Int)? = null) {
    memScoped {
        val error = this.alloc<CPointerVar<ByteVar>>()
        val callbackStable = if (callback != null) StableObjPtr.create(callback) else null
        try {
            if (sqlite3_exec(db.value, command, if (callback != null)
                        staticCFunction {
                            ptr, count, data, columns -> Int
                            val callbackFunction =
                                StableObjPtr.fromValue(ptr!!).get() as (Array<String>, Array<String>)-> Int
                            val columnsArray = fromCArray(columns!!, count)
                            val dataArray = fromCArray(data!!, count)
                            callbackFunction(columnsArray, dataArray)
                        } else null, callbackStable?.value, error.ptr) != 0)
                throw Error("DB error: ${error.value!!.toKString()}")
        } finally {
            callbackStable?.dispose()
            sqlite3_free(error.value)
        }
    }
}


fun initSession(connection: CPointer<MHD_Connection>?, db: DbConnection)  {
    // TODO: read session using cookie and DB.
    dbExecute(db, "SELECT COUNT(*) FROM clients ") {
        columns, data -> Int

        var index = 0
        while (index < columns.size) {
            println("${columns[index]} = ${data[index]}")
            index++
        }
        0
    }
}

fun main(args: Array<String>) {
    if (args.size != 1 || args[0].toIntOrNull() == null) {
        println("HttpServer <port>")
        _exit(1)
    }
    val port = args[0].toInt().toShort()
    val dbMain = dbOpen()
    dbExecute(dbMain, createDbCommand)
    val daemon = MHD_start_daemon(MHD_USE_AUTO or MHD_USE_INTERNAL_POLLING_THREAD or MHD_USE_ERROR_LOG,
        port, null, null, staticCFunction {
            cls, connection, urlC, methodC, _, _, _, _ -> Int
            // This handler could (and will) be invoked in another per-connection
            // thread, so reinit runtime.
            konan.initRuntimeIfNeeded()
            // TODO: is it correct?
            val db = cls!!.reinterpret<CPointerVar<sqlite3>>().pointed
            initSession(connection, db)
            val url = urlC!!.toKString()
            val method = methodC!!.toKString()
            println("Connection to $url method $method")
            if (method != "GET") return@staticCFunction MHD_NO
            val (contentType, responseText) = makeResponse(method, url)
            return@staticCFunction memScoped {
                val responseC = responseText.cstr
                val response = MHD_create_response_from_buffer(
                        responseC.size.toLong() - 1, responseC.getPointer(this),
                        MHD_ResponseMemoryMode.MHD_RESPMEM_MUST_COPY)
                MHD_add_response_header(response, "Content-Type", contentType)
                val result = MHD_queue_response(connection, MHD_HTTP_OK, response)
                MHD_destroy_response(response);
                result
            }
        }, dbMain.ptr,
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