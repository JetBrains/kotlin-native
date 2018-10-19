# Samples

This directory contains a set of samples demonstrating how one can work with Kotlin/Native. The samples can be
built using Gradle built tool. See `README.md` in sample directories to learn more about specific samples and
the building process.

**Note**: If the samples are built from a source tree (not from a distribution archive) the compiler built from
the sources is used. So one must build the compiler and the necessary platform libraries by running
`./gradlew bundle` from the Kotlin/Native root directory before building samples (see
[README.md](https://github.com/JetBrains/kotlin-native/blob/master/README.md) for details).

One may also build all the samples with one command. To build them using Gradle run:

    ./gradlew buildAllSamples
