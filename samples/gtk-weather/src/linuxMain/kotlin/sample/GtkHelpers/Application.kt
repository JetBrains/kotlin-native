package sample.GtkHelpers

import kotlinx.cinterop.*
import libgtk3.*
import platform.posix.exit



abstract class Application(application_id: String, gApplicationFlagsNone: GApplicationFlags) {

    var builder: CPointer<GtkBuilder>? = null
    var application: CPointer<GtkApplication>? = null

    abstract fun onActivate(app: CPointer<GtkApplication>)

    init {
        glibresources_get_resource()
        gtk_application_new(application_id, gApplicationFlagsNone)?.let { application = it }
        g_signal_connect_data(application?.reinterpret(), "activate", staticCFunction { app: CPointer<GtkApplication>, data: COpaquePointer? ->
            data?.asStableRef<(CPointer<GtkApplication>) -> Unit>()?.get()?.let { it(app) }
            null as COpaquePointer?
        }.reinterpret(), StableRef.create(::onActivate).asCPointer(), null, 0u)
    }

    fun run(args: Array<String>) {
        memScoped {
            val status = g_application_run(application?.reinterpret(), args.size, args.map { it.cstr.ptr }.toCValues())
            g_object_unref(application)

            exit(status)
        }
    }

    fun setContentView(templatePath: String) {
        builder = gtk_builder_new_from_resource(templatePath)
        val builderObjs = gtk_builder_get_objects(builder)
        //println(g_slist_length(builderObjs))
        val firstObj = g_slist_nth_data(builderObjs, 1)?.reinterpret<CPointerVar<GtkWidget>>()
        //val firstObj = gtk_builder_get_object(builder, "window")
        gtk_application_add_window(application, firstObj?.reinterpret())
        gtk_widget_show(firstObj?.reinterpret())
    }
}