fun main(args : Array<String>) {
    val arrayList = BaseArrayList<String>(5)
    var index = 0
    while (index < arrayList.size) {
        arrayList[index] = "element " + (++index)
    }
    for (it in arrayList) {
        println(it)
    }
}