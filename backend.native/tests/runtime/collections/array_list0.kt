fun main(args : Array<String>) {
    val arrayList = kotlin.collections.BaseArrayList<String>(5)
    var index = 0
    while (index < arrayList.size) {
        arrayList[index] = "element " + (++index).toString()
    }
    for (it in arrayList) {
        println(it)
    }
}