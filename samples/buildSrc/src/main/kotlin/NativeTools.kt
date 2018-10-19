@file:JvmName("NativeTools")

import java.nio.file.Paths

/*
 * This file includes short-cuts needed purely for Kotlin/Native samples.
 */

// Needed purely for test purposes:
@get:JvmName("testMavenRepoUrl")
val testMavenRepoUrl by lazy { "file://" + Paths.get(userHome, ".m2-kotlin-native-samples") }
