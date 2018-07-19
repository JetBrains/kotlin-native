import kotlinx.cinterop.*
import platform.frc.hal.*
import platform.posix.*
import kotlin.math.pow

val servoPort = 0
val potPort = 0

fun main(args: Array<String>) {
    println("Hello, FRC ${HAL_RuntimeType_e.byValue(HAL_GetRuntimeType())}!")

    val status = nativeHeap.alloc<int32_tVar>()

    println("Initializing HAL")
    HAL_Initialize(500, 0).check { it == 1 }

    println("Initializing servo on port $servoPort")
    val servo = HAL_InitializePWMPort(HAL_GetPort(servoPort), status.ptr)
    status.value.check()

    println("Initializing potentiometer on port $potPort")
    val pot = HAL_InitializeAnalogInputPort(HAL_GetPort(potPort), status.ptr)
    status.value.check()

    val matchInfo = nativeHeap.alloc<HAL_MatchInfo>()
    val controlWord = nativeHeap.alloc<HAL_ControlWord>()

    HAL_ObserveUserProgramStarting()

    println("The servo on PWM port $servoPort will 'follow' potentiometer on Analog-In port $potPort.")
    while (true) {
        HAL_WaitForDSData()
        HAL_GetMatchInfo(matchInfo.ptr).check()
        HAL_GetControlWord(controlWord.ptr).check()

        val adcReading = HAL_GetAnalogValue(pot, status.ptr)
        status.value.check()

        val servoPosition = adcReading / 4096.0

        HAL_SetPWMPosition(servo, servoPosition, status.ptr)
        status.value.check()
    }
}

inline fun Int.check(predicate: (Int) -> Boolean = { it == 0 }) {
    if (!predicate(this)) error("Call returned $this")
}