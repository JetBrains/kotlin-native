FROM openjdk:8u171-jdk-stretch as builder
WORKDIR /opt
RUN wget "https://github.com/JetBrains/kotlin-native/releases/download/v0.7.1/kotlin-native-linux-0.7.1.tar.gz"
RUN tar -xzf kotlin-native-linux-0.7.1.tar.gz
RUN rm kotlin-native-linux-0.7.1.tar.gz
RUN wget "https://gradle.org/next-steps/?version=4.7&format=bin" -O gradle-4.7-bin.zip
RUN apt-get update && apt-get -y install p7zip-full && apt-get install libcurl4-openssl-dev
RUN 7z x gradle-4.7-bin.zip && rm gradle-4.7-bin.zip

ENV KOTLIN_NATIVE_HOME "/opt/kotlin-native-linux-0.7.1"
ENV GRADLE "/opt/gradle-4.7/bin/gradle"
RUN mkdir -p /app/src/main/kotlin/org/example/weather_func
RUN mkdir -p /app/lib/cJSON-1.7.7/include/cjson
RUN mkdir -p /app/lib/cJSON-1.7.7/lib/x86_64-linux-gnu
WORKDIR /app

COPY *.def /app/
COPY *.kts /app/
COPY openweathermap_key.txt /app
COPY lib /app
COPY src /app
RUN $GRADLE wrapper --gradle-version 4.7 && $GRADLE build
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
