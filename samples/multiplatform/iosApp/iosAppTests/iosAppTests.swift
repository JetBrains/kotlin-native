//
//  iosAppTests.swift
//  iosAppTests
//
//  Created by Stefan M. on 12.04.18.
//

import XCTest
import Greeting
@testable import iosApp

class iosAppTests: XCTestCase {
    
    func testExample() {
        assert(GreetingGreeting().greeting() == "Hello, iOS")
    }
    
}
