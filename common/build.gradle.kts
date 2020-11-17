/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

plugins {
    id("compile-to-bitcode")
}

val hostName: String by project

bitcode {
    targets = listOf(hostName)

    create("Hash", file("src/hash"))
    create("Files", file("src/files"))
}

val build by tasks.registering {
    dependsOn("${hostName}Hash")
    dependsOn("${hostName}Files")
}

val clean by tasks.registering {
    doLast {
        delete(buildDir)
    }
}
