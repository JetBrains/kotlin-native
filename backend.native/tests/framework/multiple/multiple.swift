/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import First
import Second

func testClashingNames() throws {
    try assertEquals(actual: "first", expected: First.TestKt.name)
    try assertEquals(actual: "second", expected: Second.TestKt.name)

    let c1 = First.C()
    let c2 = Second.C()
    try assertTrue(type(of: c1) == First.C.self)
    try assertTrue(type(of: c2) == Second.C.self)
    try assertTrue(First.C.self != Second.C.self)
    try assertTrue(objc_getClass(class_getName(First.C.self)) as AnyObject === First.C.self)
    try assertTrue(objc_getClass(class_getName(Second.C.self)) as AnyObject === Second.C.self)
}

extension I1Impl : I2 {}

func testInteraction() throws {
    try assertEquals(actual: SecondKt.getFortyTwoFrom(i2: I1Impl()), expected: 42)
}

func testIsolation() throws {
    try assertFalse(SecondKt.isUnit(obj: FirstKt.getUnit()))

    // Ensure frameworks don't share the same runtime (state):
    try assertFalse(First.RuntimeState().consumeChange())
    try assertFalse(Second.RuntimeState().consumeChange())
    Second.RuntimeState().produceChange()
    try assertFalse(First.RuntimeState().consumeChange())
    try assertTrue(Second.RuntimeState().consumeChange())
}

class MultipleFrameworksTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        tests = [
            TestCase(name: "TestClashingNames", method: withAutorelease(testClashingNames)),
            TestCase(name: "TestInteraction", method: withAutorelease(testInteraction)),
            TestCase(name: "TestIsolation", method: withAutorelease(testIsolation)),
        ]
        providers.append(self)
    }

}