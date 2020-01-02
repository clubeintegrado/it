//
//  RiderShotsUITests.swift
//  RiderShotsUITests
//
//  Copyright © 2019 minimal. All rights reserved.
//

import XCTest

class RiderShotsUITests: XCTestCase {

    override func setUp() {
        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false
        
        // UI tests must launch the application that they test. Doing this in setup will make sure it happens for each test method.
        let app = XCUIApplication()
        setupSnapshot(app)
        app.launch()
        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
    }

    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    func testExample() {
        // Use recording to get started writing UI tests.
        
        let app = XCUIApplication()
        let element = app.children(matching: .window).element(boundBy: 0).children(matching: .other).element.children(matching: .other).element
        app.buttons["Confirm Pickup"].tap()
        element.swipeLeft()
        app.buttons["Confirm Destination"].tap()
        app.collectionViews.children(matching: .cell).element(boundBy: 0).children(matching: .other).element.children(matching: .other).element.tap()
        snapshot("01SelectServices")
        app.buttons["Confirm Sedan"].tap()
        app.buttons["back"].tap()
        app.buttons["menu"].tap()
        snapshot("02Menu")
        app.scrollViews.otherElements.buttons["Trip History"].tap()
        snapshot("03TripHistory")
        app.navigationBars["Trip History"].buttons["Back"].tap()
                // Use XCTAssert and related functions to verify your tests produce the correct results.
    }

}
