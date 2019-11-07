package sample

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.reinterpret
import libgtk3.*

object WeatherAPI {

    data class Place(val name: String,val woeid: Int)
    data class WeatherInfo(
        val currentTemp: Double,
        val maxTemp: Double,
        val minTemp: Double,
        val weatherState: String,
        val weatherStateAbbr: String,
        val windSpeed: Double,
        val humidity: Double,
        val dayOfWeek: String
    )

    fun queryLocation(placeName: String): ArrayList<Place> {
        val hashMap = arrayListOf<Place>()
        curl_read_all_content("https://www.metaweather.com/api/location/search/?query=$placeName")?.let { jsonString ->
            JSON(jsonString).asArray()?.forEachObjets {
                val place = it.getString("title")!!
                val woeid = it.getInt("woeid")
                hashMap.add(Place(placeName, woeid))
            }
        }
        return hashMap
    }

    fun getWeatherInfo(woeid: Int): ArrayList<WeatherInfo> {
        val infoArray = arrayListOf<WeatherInfo>()

        curl_read_all_content( "https://www.metaweather.com/api/location/$woeid/")?.let { jsonString ->
            JSON(jsonString).asObject()?.getArray("consolidated_weather").forEachObjets {
                val currentTemp= it.getDouble("the_temp")
                val maxTemp = it.getDouble("max_temp")
                val minTemp = it.getDouble("min_temp")
                val weatherState = it.getString("weather_state_name")
                val weatherStateAbbr = it.getString("weather_state_abbr")
                val windSpeed = it.getDouble("wind_speed")
                val humidity = it.getDouble("humidity")
                val dayOfWeek = it.getString("applicable_date")?.getWeekDay()

                infoArray.add( WeatherInfo(currentTemp, maxTemp, minTemp, weatherState ?: "", weatherStateAbbr ?: "", windSpeed, humidity, dayOfWeek ?: "") )
            }
        }
        return infoArray
    }

}


/* Universal Extionsion */
fun String.getWeekDay(): String? {
    val dayOfWeek = g_date_time_get_day_of_week(
        g_date_time_new_local(
            substring(0, 4).toInt(),
            substring(5, 7).toInt(),
            substring(8, 10).toInt(),
            0, 0, 0.0
        )
    )
    return when(dayOfWeek) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> null
    }
}


fun createElement(weekDay: String, state: String, maxT: String, minT: String): CPointer<GtkWidget>? {

    val gtkGrid = gtk_grid_new()
    gtk_widget_set_margin_left(gtkGrid?.reinterpret(), 16)
    gtk_widget_set_margin_right(gtkGrid?.reinterpret(), 16)
    gtk_widget_set_margin_top(gtkGrid?.reinterpret(), 8)
    gtk_widget_set_margin_bottom(gtkGrid?.reinterpret(), 8)
    gtk_grid_set_row_spacing(gtkGrid?.reinterpret(), 4)

    val dayLabel = gtk_label_new(weekDay)
    gtk_grid_attach(gtkGrid?.reinterpret(), dayLabel, 0, 0,1, 1)


    val icon = when(state) {
        "sl" -> "sleet"
        "lc" -> "partly_cloudy"
        "hc" -> "cloudy"
        "c" -> "sunny"
        "sn" -> "snow"
        "t" -> "thunderstorms"
        "hr" -> "rain"
        "lr" -> "rain_light"
        "s" -> "rain_s_sunny"
        "h" -> "snow"
        else -> null
    }

    val pixbuf = gtk_image_new_from_resource("/org/gtk/example/raw/$icon.png")
    gtk_grid_attach(gtkGrid?.reinterpret(), pixbuf?.reinterpret(), 0, 1,1, 1)

    val dayMaxMin = gtk_label_new("$maxT°  $minT°")
    gtk_grid_attach(gtkGrid?.reinterpret(), dayMaxMin, 0, 2,1, 1)

    return gtkGrid
}