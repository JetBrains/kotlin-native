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

import Kt

func testAccessClassFromLibraryWithShortName() throws {

     let object: MyLibraryA = MyLibraryA(data: "Data from Class")
     let interface: MyLibraryI = MyLibraryA(data: "Data from Interface")
     let enumObject: MyLibraryE = MyLibraryE.b


     let dataFromClass = LibraryKt.readDataFromLibraryClass(input: object)
     let dataFromInterface = LibraryKt.readDataFromLibraryInterface(input: interface)
     let dataFromEnum = LibraryKt.readDataFromLibraryEnum(input: enumObject)

     try assertEquals(actual: dataFromClass, expected: "Data from Class")
     try assertEquals(actual: dataFromInterface, expected: "Data from Interface")
     try assertEquals(actual: dataFromEnum, expected: "Enum entry B")
}

class LibraryTests : SimpleTestProvider {
    override init() {
        super.init()

        test("testAccessClassFromLibraryWithShortName", testAccessClassFromLibraryWithShortName)
    }
}