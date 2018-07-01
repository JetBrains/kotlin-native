group = "org.example"
version = "0.1-SNAPSHOT"

plugins {
    id("konan")
}

konanArtifacts {
    interop("curl") {
        defFile("curl.def")
    }

	program("weather") {
    	entryPoint("org.example.weather_func.main")
        libraries {
            artifact("curl")
        }
    }
}
