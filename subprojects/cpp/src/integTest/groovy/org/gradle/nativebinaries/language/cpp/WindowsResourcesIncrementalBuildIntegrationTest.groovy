/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.nativebinaries.language.cpp

import org.gradle.nativebinaries.language.cpp.fixtures.AbstractInstalledToolChainIntegrationSpec
import org.gradle.nativebinaries.language.cpp.fixtures.ExecutableFixture
import org.gradle.nativebinaries.language.cpp.fixtures.RequiresInstalledToolChain
import org.gradle.nativebinaries.language.cpp.fixtures.app.HelloWorldApp
import org.gradle.nativebinaries.language.cpp.fixtures.app.WindowsResourceHelloWorldApp

import static org.gradle.nativebinaries.language.cpp.fixtures.ToolChainRequirement.VisualCpp

@RequiresInstalledToolChain(VisualCpp)
class WindowsResourcesIncrementalBuildIntegrationTest extends AbstractInstalledToolChainIntegrationSpec {

    HelloWorldApp helloWorldApp = new WindowsResourceHelloWorldApp()
    ExecutableFixture mainExe
    def mainResourceFile
    def unusedHeaderFile

    def "setup"() {
        buildFile << helloWorldApp.pluginScript
        buildFile << helloWorldApp.extraConfiguration
        buildFile << """
            executables {
                main {}
            }
        """

        helloWorldApp.writeSources(file("src/main"))
        unusedHeaderFile = file("src/main/headers/unused.h") << """
    #define DUMMY_HEADER_FILE
"""

        run "mainExecutable"

        mainExe = executable("build/binaries/mainExecutable/main")
        mainResourceFile = file("src/main/rc/resources.rc")
    }

    def "does not re-compile sources with no change"() {
        when:
        run "mainExecutable"

        then:
        nonSkippedTasks.empty
    }

    def "compiles and links when resource source changes"() {
        when:
        file("src/main/rc/resources.rc").text = """
#include "hello.h"

STRINGTABLE
{
    IDS_HELLO, "Goodbye"
}
"""

        and:
        run "mainExecutable"

        then:
        executedAndNotSkipped ":compileMainExecutableMainRc", ":linkMainExecutable", ":mainExecutable"

        and:
        mainExe.exec().out == "Goodbye"
    }

    def "compiles and but does not link when resource source changes with comment only"() {
        when:
        file("src/main/rc/resources.rc") << """
// Comment added to the end of the resource file
"""

        and:
        run "mainExecutable"

        then:
        executedAndNotSkipped ":compileMainExecutableMainRc"
        skipped ":linkMainExecutable", ":mainExecutable"
    }

    def "compiles and links when resource compiler arg changes"() {
        when:
        buildFile << """
            executables {
                main {
                    binaries.all {
                        // Use a compiler arg that will change the generated .res file
                        rcCompiler.args "-DFRENCH"
                    }
                }
            }
"""
        and:
        run "mainExecutable"

        then:
        executedAndNotSkipped ":compileMainExecutableMainRc", ":linkMainExecutable", ":mainExecutable"
    }

    def "stale .res files are removed when a resource source file is renamed"() {
        given:
        def oldResFile = file("build/objectFiles/mainExecutable/mainRc/${hashFor(mainResourceFile)}/resources.res")
        def newResFile = file("build/objectFiles/mainExecutable/mainRc/${hashFor(file('src/main/rc/changed_resources.rc'))}/changed_resources.res")
        assert oldResFile.file
        assert !newResFile.file

        when:
        mainResourceFile.renameTo(file("src/main/rc/changed_resources.rc"))
        run "mainExecutable"

        then:
        executedAndNotSkipped ":compileMainExecutableMainRc"

        and:
        !oldResFile.file
        newResFile.file
    }

    def "recompiles resource when included header is changed"() {
        given: "set the generated res file timestamp to zero"
        def resourceFile = file("build/objectFiles/mainExecutable/mainRc/${hashFor(mainResourceFile)}/resources.res")
        resourceFile.lastModified = 0
        when: "Unused header is changed"
        unusedHeaderFile << """
    #define EXTRA_DEFINE
"""
        and:
        run "mainExecutable"

        then: "No resource compilation"
        skipped ":compileMainExecutableMainRc"
        resourceFile.lastModified() == 0

        when:
        file("src/main/headers/hello.h") << """
    #define EXTRA_DEFINE
"""
        and:
        run "mainExecutable"

        then: "Resource is recompiled"
        executedAndNotSkipped ":compileMainExecutableMainRc"
        resourceFile.lastModified() > 0
    }
}

