FROM openjdk:8u171-jdk-stretch as builder
WORKDIR /opt
ENV KOTLIN_NATIVE_VER "0.7.1"
RUN wget "https://github.com/JetBrains/kotlin-native/releases/download/v0.7.1/kotlin-native-linux-$KOTLIN_NATIVE_VER.tar.gz"
RUN tar -xzf kotlin-native-linux-$KOTLIN_NATIVE_VER.tar.gz && rm kotlin-native-linux-$KOTLIN_NATIVE_VER.tar.gz
RUN apt-get update && apt-get install libcurl4-openssl-dev

ENV KOTLIN_NATIVE_HOME "/opt/kotlin-native-linux-$KOTLIN_NATIVE_VER"
RUN mkdir -p /app/src/main/kotlin/org/example/weather_func
RUN mkdir -p /app/lib/cJSON-1.7.7/include/cjson && mkdir -p /app/lib/cJSON-1.7.7/lib/x86_64-linux-gnu
RUN mkdir -p /app/gradle/wrapper
WORKDIR /app

COPY gradle/wrapper/gradle-wrapper.properties /app/gradle/wrapper
COPY gradle/wrapper/gradle-wrapper.jar /app/gradle/wrapper
COPY gradlew /app
COPY .konan/ /root/.konan
COPY *.def /app/
COPY *.kts /app/
COPY openweathermap_key.txt /app
COPY src/ /app/src
COPY lib/cJSON-1.7.7/include/cjson/cJSON.h /app/lib/cJSON-1.7.7/include/cJSON.h
COPY lib/cJSON-1.7.7/lib/x86_64-linux-gnu/libcjson.a /app/lib/cJSON-1.7.7/lib/libcjson.a
RUN ./gradlew build
RUN cp build/konan/bin/linux_x64/weather.kexe weather

FROM debian:stretch
RUN apt-get update && apt-get -y install libcurl4-openssl-dev
ADD https://github.com/openfaas/faas/releases/download/0.8.2/fwatchdog /usr/bin
RUN chmod +x /usr/bin/fwatchdog
RUN mkdir -p /app
WORKDIR /app
COPY --from=builder /app/openweathermap_key.txt .
COPY --from=builder /app/weather .
RUN chmod +x weather
ENV fprocess "./weather"
HEALTHCHECK --interval=2s CMD [ -e /tmp/.lock ] || exit 1
CMD ["fwatchdog"]
