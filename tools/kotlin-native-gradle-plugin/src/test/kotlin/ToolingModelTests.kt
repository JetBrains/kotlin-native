/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin.test

import org.jetbrains.kotlin.konan.KonanVersion
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test

open class ToolingModelTests {
    val tmpFolder = TemporaryFolder()
        @Rule get

    val projectDirectory: File
        get() = tmpFolder.root

    @Test
    fun `The model should be serialized without exceptions`() {
        val project = KonanProject.createEmpty(projectDirectory).apply {
            buildFile.appendText("""
                konanArtifacts {
                    library('foo') {
                        srcDir 'src/foo/kotlin'
                    }
                    interop('bar')

                    program('main') {
                        libraries {
                            artifact konanArtifacts.foo
                            artifact konanArtifacts.bar
                            klib 'posix'
                        }
                    }
                }

                import org.jetbrains.kotlin.gradle.plugin.model.*

                task testSerialization {
                    doLast {
                        def model = KonanToolingModelBuilder.INSTANCE.buildAll("KonanModel", project)
                        file("model.bin").withObjectOutputStream {
                            it.writeObject(model)
                        }

                        KonanModelImpl deserializedModel
                        file("model.bin").withObjectInputStream(model.getClass().getClassLoader()) { stream ->
                            deserializedModel = (KonanModelImpl) stream.readObject()
                        }

                        if (!deserializedModel.equals(model)) {
                            throw new AssertionError("The deserialized model doesn't equal to the initial one")
                        }
                    }
                }
            """.trimIndent())

            generateSrcFile("main.kt")
            generateSrcFile(Paths.get("src", "foo", "kotlin"), "foo.kt", "fun foo() = 1")
            generateSrcFile(Paths.get("src", "bar", "kotlin"), "bar.kt", "fun bar() = 1")
            generateDefFile("baz.def", "")
            generateDefFile("qux.def", "")
        }
        project.createRunner().withArguments("testSerialization").build()
    }

    @Test
    fun `The model should contain the same data as the Gradle tasks`() {
        val project = KonanProject.createEmpty(projectDirectory, listOf("host", "wasm32")).apply {
            val konanVersion = KonanVersion.CURRENT.toString()
            generateSrcFile(listOf("src", "foo1"), "foo1.kt", "fun foo1() = 0")
            generateSrcFile(listOf("src", "foo1"), "foo11.kt", "fun foo11() = 0")
            generateSrcFile(listOf("src", "foo2"), "foo2.kt", "fun foo2() = 0")
            generateSrcFile(listOf("defs_bar"), "bar.def", "")
            generateSrcFile(listOf("src", "baz"), "baz.kt", "fun baz() = 0")
            generateSrcFile("main.kt")
            buildFile.appendText("""
                konanArtifacts {
                    library('foo') {
                        srcDir 'src/foo1'
                        srcDir 'src/foo2'
                    }
                    interop('bar') {
                        defFile 'defs_bar/bar.def'
                    }
                    library('baz') {
                        srcDir 'src/baz'
                    }

                    program('main') {
                        libraries {
                            artifact konanArtifacts.foo
                            artifact konanArtifacts.bar
                            klib 'baz'
                        }
                    }
                }

                konan {
                    languageVersion = "1.2"
                    apiVersion = "1.2"
                }

                import org.jetbrains.kotlin.gradle.plugin.model.*
                import org.jetbrains.kotlin.gradle.plugin.tasks.*

                public <T> void assertEquals(
                        T actual,
                        T expected,
                        String message = "${'$'}expected expected but ${'$'}actual found") {
                    if (expected != actual) {
                        throw new AssertionError(message)
                    }
                }

                public <T> void assertContentEquals(
                        Collection<T> actual,
                        Collection<T> expected,
                        String message = "${'$'}expected\nexpected but\n ${'$'}actual\nfound") {
                    if (actual.size() != expected.size() || !actual.containsAll(expected)) {
                        throw new AssertionError(message)
                    }
                }

                task testModelData {
                    dependsOn('compileKonanBaz')
                    doLast {
                        def model = KonanToolingModelBuilder.INSTANCE.buildAll("KonanModel", project)
                        assertEquals(model.konanHome, file(project.getProperty('konan.home')))
                        assertEquals(model.konanVersion.toString(), "$konanVersion")

                        assertEquals(model.languageVersion, "1.2")
                        assertEquals(model.apiVersion, "1.2")

                        model.artifacts.each {

                            def konanArtifact = konanArtifacts[it.name]
                            def target = it.targetPlatform
                            def task = konanArtifact.getByTarget(target)

                            assertEquals(it.file, task.artifact)
                            assertEquals(it.buildTaskName, task.name)

                            switch(it.name) {
                                case 'foo':
                                    assertEquals(it.type, Produce.LIBRARY)
                                    assertContentEquals(it.srcDirs, [file('src/foo1'), file('src/foo2')])
                                    assertContentEquals(it.srcFiles, [
                                            file('src/foo1/foo1.kt'),
                                            file('src/foo1/foo11.kt'),
                                            file('src/foo2/foo2.kt')])
                                    assertContentEquals(it.libraries, [])
                                    break
                                case 'bar':
                                    assertEquals(it.type, Produce.LIBRARY)
                                    assertContentEquals(it.srcDirs, [file('defs_bar')])
                                    assertContentEquals(it.srcFiles, [file('defs_bar/bar.def')])
                                    assertContentEquals(it.libraries, [])
                                    break
                                case 'main':
                                    assertEquals(it.type, Produce.PROGRAM)
                                    assertContentEquals(it.srcDirs, [file('src/main/kotlin')])
                                    assertContentEquals(it.srcFiles, [file('src/main/kotlin/main.kt')])
                                    println(it.libraries)
                                    assertContentEquals(it.libraries, [
                                            konanArtifacts['foo'].getByTarget(target).artifact,
                                            konanArtifacts['bar'].getByTarget(target).artifact,
                                            konanArtifacts['baz'].getByTarget(target).artifact
                                    ])
                                    break
                            }
                        }
                    }
                }
            """.trimIndent())
        }
        project.createRunner().withArguments("testModelData").build()
    }
}
