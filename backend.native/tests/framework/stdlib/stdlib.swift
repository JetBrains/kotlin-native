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

import Foundation
import Stdlib

class StdlibTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        tests = [
            TestCase(name: "TestEmptyDictionary", method: withAutorelease(testEmptyDictionary)),
            TestCase(name: "TestGenericMapUsage", method: withAutorelease(testGenericMapUsage)),
            TestCase(name: "TestOrderedMapStored", method: withAutorelease(testOrderedMapStored)),
            TestCase(name: "TestTypedMapUsage", method: withAutorelease(testTypedMapUsage)),
            TestCase(name: "TestFirstElement", method: withAutorelease(testFirstElement)),
            TestCase(name: "TestAddDictionary", method: withAutorelease(testAddDictionary))
        ]
        providers.append(self)
    }

    /**
     * Pass empty dictionary to Kotlin.
     */
    func testEmptyDictionary() throws {
        let immutableEmptyDict = [String: Int]()
        try assertTrue(Stdlib.isEmpty(map: immutableEmptyDict), "Empty dictionary")
        let keys = Stdlib.getKeysAsSet(map: immutableEmptyDict)
        try assertTrue(keys.isEmpty, "Should have empty set")
    }

    /**
     * Tests usage of a map with generics.
     */
    func testGenericMapUsage() throws {
        let map = Stdlib.createLinkedMap()
        map[1] = "One"
        map[10] = "Ten"
        map[11] = "Eleven"
        map["10"] = "Ten as string"
        for (k, v) in map {
            print("MAP: \(k) - \(v)")
        }

        try assertEquals(actual: map[11] as! String, expected: "Eleven", "An element of the map for key: 11")
    }

    /**
     * Checks order of the underlying LinkedHashMap.
     */
    func testOrderedMapStored() throws {
        let pair = Stdlib.createPair()
        let map = pair.first as? NSMutableDictionary

        map?[1] = "One"
        map?[10] = "Ten"
        map?[11] = "Eleven"
        map?["10"] = "Ten as string"

        let gen = pair.second as! StdlibGenericExtensionClass
        let value: String? = gen.getFirstValue() as? String
        try assertEquals(actual: value!, expected: "One", "First value of the map")

        let key: Int? = gen.getFirstKey() as? Int
        try assertEquals(actual: key!, expected: 1, "First key of the map")
    }

    /**
     * Tests typed map created in Kotlin.
     */
    func testTypedMapUsage() throws {
        let map = Stdlib.createTypedMutableMap()
        map[1] = "One"
        map[1.0 as Float] = "Float"
        map[11] = "Eleven"
        map["10"] = "Ten as string"
        
        try assertEquals(actual: map["10"] as! String, expected: "Ten as string", "String key")
        try assertEquals(actual: map[1.0 as Float] as! String, expected: "Float", "Float key")
    }
    
    /**
     * Get first element of the collection.
     */
    func testFirstElement() throws {
        let m = Stdlib.createTypedMutableMap()
        m[10] = "Str"
        try assertEquals(actual: Stdlib.getFirstElement(collection: m.allKeys) as! Int, expected: 10, "First key")

        try assertEquals(actual: Stdlib.getFirstElement(collection: Stdlib.getKeysAsList(map: m as! Dictionary)) as! Int,
                expected: 10, "First key from a list")
    }

    /**
     * Add element to dictionary in Kotlin
     */
    func testAddDictionary() throws {
        var m = [ "ABC": 10, "CDE": 12, "FGH": 3 ]
        Stdlib.addSomeElementsToMap(map: StdlibMutableDictionary(dictionary: m))
        for (k, v) in m {
            print("MAP: \(k) - \(v)")
        }

        var smd = StdlibMutableDictionary<NSString, NSNumber>()
        smd.setObject(333, forKey: "333" as NSString)
        try assertEquals(actual: smd.object(forKey: "333" as NSString) as! Int, expected: 333, "Add element to dict")
        
        Stdlib.addSomeElementsToMap(map: smd)
        for (k, v) in smd {
            print("MAP: \(k) - \(v)")
        }
        try assertEquals(actual: smd.object(forKey: "XYZ" as NSString) as! Int, expected: 321, "Get element from Kotlin")
    }
}
