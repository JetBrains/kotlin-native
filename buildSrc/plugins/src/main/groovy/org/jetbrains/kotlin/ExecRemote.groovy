/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.util.ConfigureUtil

// This class provides process.execRemote -- a drop-in replacement for process.exec .
// If we provide -Premote=user@host the binaries are executed on a remote host
// If we omit -Premote then the binary is executed locally as usual.

class ExecRemote {

    private final Project project
    private final String remote = null
    private final String remoteDir = null

    ExecRemote(Project project) {
        this.project = project
         if (project.hasProperty('remote')) {
             remote = project.ext.remote
             remoteDir = uniqSessionName()
             createRemoteDir()
         }
    }

    private String uniqSessionName() {
        def date = new Date().format('yyyyMMddHHmmss')
        return project.ext.remoteRoot + File.createTempFile(System.properties['user.name'], "_" + date)
    }

    private createRemoteDir() {
        project.exec {
             commandLine('ssh', remote, 'mkdir', '-p', remoteDir)
        }
    }

    private upload(String fileName) {
        project.exec {
            commandLine('scp', fileName, "${remote}:${remoteDir}")
        }
    }

    public ExecResult execRemote(Action<? super ExecSpec> action) {
        Action<? super ExecSpec> extendedAction = new Action<ExecSpec>() {
            @Override
            void execute(ExecSpec execSpec) {
                action.execute(execSpec)

                if (remote == null) return

                execSpec.with {
                    upload(getExecutable())
                    executable = "$remoteDir/${new File(executable).name}"
                    if (project.hasProperty('remote')) {
                        commandLine = ['/usr/bin/ssh', remote] + commandLine
                    }
                }
            }
        }
        return project.exec(extendedAction)
    }

    public ExecResult execRemote(Closure closure) {
        return this.execRemote(ConfigureUtil.configureUsing(closure));
    }
}
