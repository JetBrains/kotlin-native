group = "org.example"
version = "0.1-SNAPSHOT"

plugins {
    id("konan")
}

konanArtifacts {
	program("weather") {
    	entryPoint("org.example.weather_func.main")
    }
}
