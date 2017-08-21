import org.jetbrains.ring.Launcher

fun main(args: Array<String>) {
    var numWarmIterations    =  0       // Should be 100000 for jdk based run
    var numMeasureIterations =  2000

    if (args.size == 2) {
        numWarmIterations    = args[0].toInt()
        numMeasureIterations = args[1].toInt()
    }

    println("Ring starting")
    println("  warmup  iterations count: $numWarmIterations")
    println("  measure iterations count: $numMeasureIterations")
    Launcher(numWarmIterations, numMeasureIterations).runBenchmarks()
}