/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation

class Blackhole {
    static var consumer = 0
    static func consume<T: Hashable>(_ value: T) {
        consumer = value.hashValue
    }
}

struct Constants {
    static let BENCHMARK_SIZE = 10000
    static let RUNS = 1_000_000
    static var globalAddendum = 0
}
