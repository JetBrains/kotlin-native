package org.example.weather_func

import platform.posix.*
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlin.system.exitProcess

private val API_KEY by lazy { fetchApiKey() }

fun main(args: Array<String>) {
	val location = fetchLocationArg(args)

	if (location.isNotEmpty()) {
		printFromWeatherService(location)
	} else {
		handleEmptyInput()
	}
	println("Exiting...")
}

private fun handleEmptyInput() {
	println(
		"""
		| Weather - A Serverless Function that outputs weather information.
		| Usage examples (pass one of the following as input to the function):
		|   * Location (uses the Open Weather Map service), eg:
		|       -l="christchurch,nz"
		""".trimMargin()
	)
	exitProcess(0)
}

private fun checkFile(file: String) {
	val error = -1
	if (access(file, F_OK) == error) {
		println("File $file doesn't exist!")
		exitProcess(error)
	}
}

private fun printFromWeatherService(location: String) {
	println("Fetching weather information (for $location)...")
	val curl = CUrl(createUrl(location)).apply {
		header += { if(it.startsWith("HTTP")) println("Response Status: $it") }
		body += { println("Weather information:\n$it") }
	}
	curl.fetch()
	curl.close()
}

private fun fetchLocationArg(args: Array<String>): String {
	val flag = "-l"
	return if (args.size == 1 && args[0].startsWith("$flag=")) {
		args[0].replace("$flag=", "").replace("\"", "")
	} else {
		""
	}
}

private fun createUrl(location: String): String {
	val baseUrl = "http://api.openweathermap.org/data/2.5/weather"
	return "$baseUrl?q=$location&units=metric&appid=$API_KEY"
}

private fun fetchApiKey(): String {
	var result = ""
	// bufferLength in bytes.
	val bufferLength = 50 * 8
	// Open the file using the fopen function and store the file handle.
	val file = fopen("openweathermap_key.txt", "r")

	memScoped {
		val buffer = allocArray<ByteVar>(bufferLength)
		// Read a line from the file using the fgets function.
		result = fgets(buffer, bufferLength, file)?.toKString()?.replace("\n", "") ?: ""
	}
	// Close the file.
	fclose(file)
	return result
}
