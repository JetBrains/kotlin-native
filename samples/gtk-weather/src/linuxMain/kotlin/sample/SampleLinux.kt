package sample

import kotlinx.cinterop.*
import libgtk3.*
import sample.GtkHelpers.Application


class SampleLinux(s: String, gApplicationFlagsNone: GApplicationFlags) : Application(s, gApplicationFlagsNone) {

    override fun onActivate(app: CPointer<GtkApplication>) {
        setContentView("/org/gtk/example/layout/main.ui")

        val window = gtk_application_get_active_window(application)
        gtk_window_set_title(window, "Weather")



        createThread {
            val woeid = WeatherAPI.queryLocation("Toronto")
            val weekForcast = WeatherAPI.getWeatherInfo(woeid.get(0).woeid)

            val todayInfo = weekForcast.get(0)

            gtk_ideal {
                gtk_label_set_text(gtk_builder_get_object(builder, "the_temp")?.reinterpret(), "${todayInfo.currentTemp.toInt()}°")
                gtk_label_set_text(gtk_builder_get_object(builder, "day_place")?.reinterpret(), "${todayInfo.dayOfWeek} - ${woeid.get(0).name}")
                gtk_label_set_text(gtk_builder_get_object(builder, "weather_state_name")?.reinterpret(), "${todayInfo.weatherState}")
                gtk_label_set_text(gtk_builder_get_object(builder, "more_detailed")?.reinterpret(), "High: ${todayInfo.maxTemp.toInt()}° Low: ${todayInfo.minTemp.toInt()}°  Precip: 0%")


                val weekGridView = gtk_builder_get_object(builder, "week_detail_grid")
                weekForcast.drop(1).forEachIndexed { index, dayInfo ->
                    gtk_grid_attach(weekGridView?.reinterpret(), createElement(dayInfo.dayOfWeek.substring(0,3), dayInfo.weatherStateAbbr, dayInfo.maxTemp.toInt().toString(), dayInfo.minTemp.toInt().toString())?.reinterpret(), index, 0, 1, 1)
                }
                gtk_widget_show_all(weekGridView?.reinterpret())

            }

        }

    }



}







/** Main Method **/
fun main(args: Array<String>) {
    SampleLinux("com.gtk.example", G_APPLICATION_FLAGS_NONE).run(args)
}