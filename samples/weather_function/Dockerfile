FROM openjdk:8u171-jdk-stretch as builder
WORKDIR /opt
ENV KOTLIN_NATIVE_VER "0.7.1"
RUN wget "https://github.com/JetBrains/kotlin-native/releases/download/v0.7.1/kotlin-native-linux-$KOTLIN_NATIVE_VER.tar.gz"
RUN tar -xzf kotlin-native-linux-$KOTLIN_NATIVE_VER.tar.gz && rm kotlin-native-linux-$KOTLIN_NATIVE_VER.tar.gz
RUN apt-get update && apt-get install libcurl4-openssl-dev

ENV KOTLIN_NATIVE_HOME "/opt/kotlin-native-linux-$KOTLIN_NATIVE_VER"
ENV GRADLE "/opt/gradle-4.7/bin/gradle"
RUN mkdir -p /app/src/main/kotlin/org/example/weather_func
RUN mkdir -p /app/lib/cJSON-1.7.7/include/cjson
RUN mkdir -p /app/lib/cJSON-1.7.7/lib/x86_64-linux-gnu
RUN mkdir -p /app/gradle
WORKDIR /app

COPY ../gradle/wrapper /app/gradle/wrapper
COPY ../gradlew /app
COPY *.def /app/
COPY *.kts /app/
COPY openweathermap_key.txt /app
COPY src /app
RUN ./gradlew build
RUN cp build/konan/bin/linux_x64/weather.kexe weather

FROM frolvlad/alpine-glibc
RUN apk add curl-dev --update-cache --repository http://dl-3.alpinelinux.org/alpine/edge/main/ --allow-untrusted
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
