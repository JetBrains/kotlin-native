package sample


import kotlinx.cinterop.*
import libgtk3.*


fun JSON(string: String): CPointer<JsonNode>? {
    val parser = json_parser_new()
    val bool = json_parser_load_from_data(parser, string, string.length.convert(), null)
    return json_parser_get_root(parser)
}



fun CPointer<JsonNode>?.get() = json_node_get_parent(this)

fun  CPointer<JsonNode>?.asArray() = json_node_get_array(this)
fun CPointer<JsonNode>?.asObject() = json_node_get_object(this)




fun CPointer<JsonArray>?.forEachObjets(func: (CPointer<JsonObject>) -> Unit) {
    val arraySize = json_array_get_length(this).toUInt()
    var i = 0u
    while (i < arraySize) {
        json_array_get_object_element(this, i)?.let { func(it) }
        i++
    }
}




fun CPointer<JsonArray>.getObject(index: Int) = json_array_get_object_element(this, index.toUInt())
fun CPointer<JsonArray>.getArray(index: Int) = json_array_get_array_element(this, index.toUInt())

fun CPointer<JsonObject>.getObject(key: String) = json_object_get_object_member(this, key)
fun CPointer<JsonObject>.getArray(key: String) = json_object_get_array_member(this, key)

fun CPointer<JsonObject>.getInt(key: String) = json_object_get_int_member(this, key).toInt()
fun CPointer<JsonObject>.getDouble(key: String) = json_object_get_double_member(this, key).toDouble()
fun CPointer<JsonObject>.getString(key: String) = json_object_get_string_member(this, key)?.toKString()
fun CPointer<JsonObject>.getBoolean(key: String) = json_object_get_boolean_member(this, key) == TRUE