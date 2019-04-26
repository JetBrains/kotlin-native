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
import ValuesGenerics

// -------- Tests --------

func testVararg() throws {
    let ktArray = KotlinArray<KotlinInt>(size: 3, init: { (_) -> KotlinInt in return KotlinInt(int:42) })
    let arr: [Int] = ValuesKt.varargToList(args: ktArray as! KotlinArray<AnyObject>) as! [Int]
    try assertEquals(actual: arr, expected: [42, 42, 42])
}

func testDataClass() throws {
    let f = "1" as NSString
    let s = "2" as NSString
    let t = "3" as NSString

    let tripleVal = TripleVals<NSString>(first: f, second: s, third: t)
    try assertEquals(actual: tripleVal.first, expected: f, "Data class' value")
    try assertEquals(actual: tripleVal.first, expected: "1", "Data class' value literal")
    try assertEquals(actual: tripleVal.component2(), expected: s, "Data class' component")
    print(tripleVal)
    try assertEquals(actual: String(describing: tripleVal), expected: "TripleVals(first=\(f), second=\(s), third=\(t))")

    let tripleVar = TripleVars<NSString>(first: f, second: s, third: t)
    try assertEquals(actual: tripleVar.first, expected: f, "Data class' value")
    try assertEquals(actual: tripleVar.component2(), expected: s, "Data class' component")
    print(tripleVar)
    try assertEquals(actual: String(describing: tripleVar), expected: "[\(f), \(s), \(t)]")

    tripleVar.first = t
    tripleVar.second = f
    tripleVar.third = s
    try assertEquals(actual: tripleVar.component2(), expected: f, "Data class' component")
    try assertEquals(actual: String(describing: tripleVar), expected: "[\(t), \(f), \(s)]")
}

func testInlineClasses() throws {
    let ic1: Int32 = 42
    let ic1N = ValuesKt.box(ic1: 17)
    let ic2 = "foo"
    let ic2N = "bar"
    let ic3 = TripleVals<AnyObject>(first: KotlinInt(int:1), second: KotlinInt(int:2), third: KotlinInt(int:3))
    let ic3N = ValuesKt.box(ic3: nil)

    try assertEquals(
        actual: ValuesKt.concatenateInlineClassValues(ic1: ic1, ic1N: ic1N, ic2: ic2, ic2N: ic2N, ic3: ic3, ic3N: ic3N),
        expected: "42 17 foo bar TripleVals(first=1, second=2, third=3) null"
    )

    try assertEquals(
        actual: ValuesKt.concatenateInlineClassValues(ic1: ic1, ic1N: nil, ic2: ic2, ic2N: nil, ic3: nil, ic3N: nil),
        expected: "42 null foo null null null"
    )

    try assertEquals(actual: ValuesKt.getValue1(ic1), expected: 42)
    try assertEquals(actual: ValuesKt.getValueOrNull1(ic1N) as! Int, expected: 17)

    try assertEquals(actual: ValuesKt.getValue2(ic2), expected: "foo")
    try assertEquals(actual: ValuesKt.getValueOrNull2(ic2N), expected: "bar")

    try assertEquals(actual: ValuesKt.getValue3(ic3), expected: ic3)
    try assertEquals(actual: ValuesKt.getValueOrNull3(ic3N), expected: nil)
}

func testGeneric() throws {
    let a = SomeGeneric<SomeData>(t: SomeData(num: 52))
    try assertEquals(actual: a.myVal()?.num, expected: 52)

    let nulls = GenOpen<SomeData>(arg: SomeData(num: 62))
    try assertEquals(actual: nulls.arg?.num, expected: 62)

    let isnull = GenOpen<SomeData>(arg: nil)
    try assertEquals(actual: isnull.arg, expected: nil)

    let nonnulls = GenNonNull<SomeData>(arg: SomeData(num: 72))
    try assertEquals(actual: nonnulls.arg.num, expected: 72)
    try assertEquals(actual: (Values_genericsKt.starGeneric(arg: nonnulls as! GenNonNull<AnyObject>) as! SomeData).num, expected: 72)

    let sd = SomeData(num: 33)
    let nullColl = GenCollectionsNull<SomeData>(arg: sd, coll: [sd])
    let nonNullColl = GenCollectionsNonNull<SomeData>(arg: sd, coll: [sd])

    try assertEquals(actual: (nullColl.coll[0] as! SomeData).num, expected: 33)
    try assertEquals(actual: nonNullColl.coll[0].num, expected: 33)
    try assertEquals(actual: nonNullColl.arg.num, expected: 33)

    let mixed = GenNullability<SomeData>(arg: sd, nArg: sd)
    try assertEquals(actual: mixed.asNullable()?.num, expected: 33)
    try assertEquals(actual: mixed.pAsNullable?.num, expected: 33)
}

// Swift ignores the variance and lets you force-cast to whatever you need, for better or worse.
// This would *not* work with direct Swift interop.
func testGenericVariance() throws {
    let sd = SomeData(num: 22)

    let variOut = GenVarOut<SomeData>(arg: sd)
    let variOutAny : GenVarOut<BaseData> = variOut as! GenVarOut<BaseData>
    let variOutOther : GenVarOut<SomeOtherData> = variOut as! GenVarOut<SomeOtherData>

    let variOutCheck = "variOut: \(variOut.arg.asString()), variOutAny: \(variOutAny.arg.asString()), variOutOther: \(variOutOther.arg.asString())"
    try assertEquals(actual: variOutCheck, expected: "variOut: 22, variOutAny: 22, variOutOther: 22")

    let variIn = GenVarIn<SomeData>(tArg: sd)
    let variInAny : GenVarIn<BaseData> = variIn as! GenVarIn<BaseData>
    let variInOther : GenVarIn<SomeOtherData> = variIn as! GenVarIn<SomeOtherData>

    let varInCheck = "variIn: \(variIn.valString()), variInAny: \(variInAny.valString()), variInOther: \(variInOther.valString())"
    try assertEquals(actual: varInCheck, expected: "variIn: SomeData(num=22), variInAny: SomeData(num=22), variInOther: SomeData(num=22)")

    let variCoType:GenVarOut<BaseData> = Values_genericsKt.variCoType()
    try assertEquals(actual: "890", expected: variCoType.arg.asString())

    let variContraType:GenVarIn<SomeData> = Values_genericsKt.variContraType()
    try assertEquals(actual: "SomeData(num=1890)", expected: variContraType.valString())
}

// Swift should completely ignore this, as should objc. Really verifying that the header generator
// deals with this
func testGenericUseSiteVariance() throws {
    let sd = SomeData(num: 22)

    let varUse = GenVarUse<BaseData>(arg: sd)
    let varUseArg = GenVarUse<BaseData>(arg: sd)

    varUse.varUse(a: varUseArg, b: GenVarUse<SomeData>(arg: sd) as! GenVarUse<BaseData>)
}

func testGenericInterface() throws {
    let a: NoGeneric = SomeGeneric<SomeData>(t: SomeData(num: 52))
    try assertEquals(actual: (a.myVal() as! SomeData).num, expected: 52)
}

func testGenericInheritance() throws {
    let ge = GenEx<SomeData, SomeOtherData>(myT:SomeOtherData(str:"Hello"), baseT:SomeData(num: 11))
    try assertEquals(actual: ge.t.num, expected: 11)
    try assertEquals(actual: ge.myT.str, expected: "Hello")
    let geBase = ge as GenBase<SomeData>
    try assertEquals(actual: geBase.t.num, expected: 11)

    let geAny = GenExAny<SomeData, SomeOtherData>(myT:SomeOtherData(str:"Hello"), baseT:SomeData(num: 131))
    try assertEquals(actual: (geAny.t as! SomeData).num, expected: 131)
    let geBaseAny = geAny as! GenBase<SomeData>
    try assertEquals(actual: geBaseAny.t.num, expected: 131)
}

func testGenericInnerClass() throws {

    let nestedClass = GenOuter.GenNested<SomeData>(b: SomeData(num: 543))
    try assertEquals(actual: nestedClass.b.num, expected: 543)

    let innerClass = GenOuter.GenInner<SomeData>(GenOuter<SomeOtherData>(a: SomeOtherData(str: "ggg")) as! GenOuter<AnyObject>, c: SomeData(num: 66), aInner: SomeOtherData(str: "ttt") as AnyObject)
    try assertEquals(actual: innerClass.c.num, expected: 66)

    let nestedClassSame = GenOuterSame.GenNestedSame<SomeData>(a: SomeData(num: 545))
    try assertEquals(actual: nestedClassSame.a.num, expected: 545)

    let innerClassSame = GenOuterSame.GenInnerSame<SomeOtherData>(GenOuterSame<SomeData>(a: SomeData(num: 44)) as! GenOuterSame<AnyObject>, a: SomeOtherData(str: "rrr"))
    try assertEquals(actual: innerClassSame.a.str, expected: "rrr")
}

func testGenericClashing() throws {
    let gcId = GenClashId<SomeData, SomeOtherData>(arg: SomeData(num: 22), arg2: SomeOtherData(str: "lll"))
    try assertEquals(actual: gcId.x() as! NSString, expected: "Foo")
    try assertEquals(actual: gcId.arg.num, expected: 22)
    try assertEquals(actual: gcId.arg2.str, expected: "lll")

    let gcClass = GenClashClass<SomeData, SomeOtherData, NSString>(arg: SomeData(num: 432), arg2: SomeOtherData(str: "lll"), arg3: "Bar")
    try assertEquals(actual: gcClass.int(), expected: 55)
    try assertEquals(actual: gcClass.sd().num, expected: 88)
    try assertEquals(actual: gcClass.list()[1].num, expected: 22)
    try assertEquals(actual: gcClass.arg.num, expected: 432)
    try assertEquals(actual: gcClass.clash().str, expected: "aaa")
    try assertEquals(actual: gcClass.arg2.str, expected: "lll")
    try assertEquals(actual: gcClass.arg3, expected: "Bar")

    //GenClashNames uses type parameter names that force the Objc class name itself to be mangled. Swift keeps names however
    let clashNames = GenClashNames<SomeData, SomeData, SomeData, SomeData>()
    try assertEquals(actual: clashNames.foo().str, expected: "nnn")
    try assertEquals(actual: clashNames.bar().str, expected: "qqq")
    try assertTrue(clashNames.baz(arg: ClashnameParam(str: "meh")), "ClashnameParam issue")

    let clashNamesEx = GenClashEx<SomeData>()

    let geClash = GenExClash<SomeOtherData>(myT:SomeOtherData(str:"Hello"))
    try assertEquals(actual: geClash.t.num, expected: 55)
    try assertEquals(actual: geClash.myT.str, expected: "Hello")
}

func testGenericExtensions() throws {
    let gnn = GenNonNull<SomeData>(arg: SomeData(num: 432))
    try assertEquals(actual: (gnn.foo() as! SomeData).num, expected: 432)
}

// -------- Execution of the test --------

class ValuesGenericsTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "TestVararg", method: withAutorelease(testVararg)),
            TestCase(name: "TestDataClass", method: withAutorelease(testDataClass)),
            TestCase(name: "TestInlineClasses", method: withAutorelease(testInlineClasses)),
            TestCase(name: "TestGeneric", method: withAutorelease(testGeneric)),
            TestCase(name: "TestGenericVariance", method: withAutorelease(testGenericVariance)),
            TestCase(name: "TestGenericUseSiteVariance", method: withAutorelease(testGenericUseSiteVariance)),
            TestCase(name: "TestGenericInheritance", method: withAutorelease(testGenericInheritance)),
            TestCase(name: "TestGenericInterface", method: withAutorelease(testGenericInterface)),
            TestCase(name: "TestGenericInnerClass", method: withAutorelease(testGenericInnerClass)),
            TestCase(name: "TestGenericClashing", method: withAutorelease(testGenericClashing)),
            TestCase(name: "TestGenericExtensions", method: withAutorelease(testGenericExtensions)),
        ]
    }
}
