# Weather Function Sample

This is a Serverless Function that fetches the current weather information. Part of the Function's output includes HTTP response headers, and the HTTP response body. This sample is designed to be deployed on OpenFaaS via a Docker image.

This document assumes that you are using a PC running Debian (v9 or later), Ubuntu (v14.04 or later), or Linux Mint (v17 or later).


# Building Docker Image

It will be assumed that Docker is already installed.

1. Clone the Kotlin Native Git repository: ```git clone https://github.com/JetBrains/kotlin-native ~/repos/kotlin-native```
2. Change working directory to the sample:
```cd ~/repos/kotlin-native/samples/weather_function```
3. Create the **openweathermap_key.txt** file and append your [Open Weather Map API key](https://openweathermap.org/appid) to the file
4. Build Docker image: ```docker build --t weather .```


# Usage

Make sure that you have completed the *Building Docker Image* section, and that OpenFaaS is [installed](https://docs.openfaas.com/deployment/) along with the [OpenFaaS CLI](https://github.com/openfaas/faas-cli) before proceeding.

1. Start OpenFaaS
2. Goto the following to access the OpenFaaS Portal:
http://127.0.0.1:8080
3. Click on **Deploy New Function** button
4. Select **MANUALLY** tab
5. For **Docker image** enter in **weather**
6. For **Function name** enter in **weather**
7. Click on **DEPLOY** button
8. Select the **weather** function
9. For **Request body** enter in the location: 
**-l="christchurch,nz"**
10. Click on **INVOKE** button

Do note that the program (weather) can print weather information from a JSON file, eg: ```./weather -f="current_weather.json"```, but this functionality isn't available in the Serverless Function.
