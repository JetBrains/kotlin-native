

class AAA<T>(ttt: T) {
    var xxx = ttt
    fun yyy(sss: T) {
        xxx = sss
    }
}

@Specialization("kclass:AAA", "Byte")
class BBB(ttt: Byte) {
    var xxx = ttt
    fun yyy(sss: Byte) {
        xxx = sss
    }
}

fun <T> ccc(uuu: AAA<T>) {
}

fun main(args : Array<String>) {
   val ddd = BBB(7)
   val aaa = AAA<Byte>(17)
   var kkk = AAA<Byte>(13)
   kkk = AAA<Byte>(11)

   var mmm = aaa
   mmm = kkk

   aaa.xxx = 19
   val zzz = aaa.xxx
   println(zzz)
   aaa.yyy(17)
   var www = aaa.xxx
   println(www)
   www = aaa.xxx
   println(www)

   // That would be a hazard
   // Should we throw or cancel the specialization or silently allow?
   // ccc(aaa)

}
