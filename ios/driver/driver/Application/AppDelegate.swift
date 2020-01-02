//
//  AppDelegate.swift
//  Driver
//
//  Copyright © 2018 minimalistic apps. All rights reserved.
//

import UIKit
import Firebase
import Fabric
import Crashlytics
import FirebaseUI
import Stripe
import Braintree


@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    static var info : [String:Any] {
        get {
            let path = Bundle.main.path(forResource: "Info", ofType: "plist")!
            return NSDictionary(contentsOfFile: path) as! [String: Any]
        }
    }
    var window: UIWindow?
    var launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    
    override init() {
        super.init()
        UIApplication.shared.isIdleTimerDisabled = true
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        FUIAuth.defaultAuthUI()?.auth?.setAPNSToken(deviceToken, type: AuthAPNSTokenType.unknown)
    }
    
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        FUIAuth.defaultAuthUI()?.auth?.canHandleNotification(userInfo)
    }
    
    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        if url.scheme?.localizedCaseInsensitiveCompare(Bundle.main.bundleIdentifier! + ".payments") == .orderedSame {
            return BTAppSwitch.handleOpen(url, options: options)
        }
        return (FUIAuth.defaultAuthUI()?.handleOpen(url, sourceApplication: nil))!
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        FirebaseApp.configure()
        self.launchOptions = launchOptions
        self.window?.tintColor = UIColor(hex: 0x009688)
        BTAppSwitch.setReturnURLScheme(Bundle.main.bundleIdentifier! + ".payments")
        UIApplication.shared.isIdleTimerDisabled = true
        Fabric.with([Crashlytics.self])
        // Override point for customization after application launch.
        return true
    }

    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
        SocketNetworkDispatcher.instance.disconnect()
    }
    
    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
        guard let token = UserDefaultsConfig.jwtToken else {
            return
        }
        if let presented = self.window?.rootViewController?.presentedViewController {
            LoadingOverlay.shared.showOverlay(view: presented.view)
        } else {
            LoadingOverlay.shared.showOverlay(view: self.window?.rootViewController?.view)
        }
        InstanceID.instanceID().instanceID { (result, error) in
            SocketNetworkDispatcher.instance.connect(namespace: .Driver, token: token, notificationId: result?.token ?? "") { result in
                LoadingOverlay.shared.hideOverlayView()
                switch result {
                case .success(_):
                    NotificationCenter.default.post(name: .connectedAfterForeground, object: nil)
                    
                case .failure(let error):
                    NotificationCenter.default.post(name: .connectionError, object: error)
                    break
                }
            }
        }
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
        SocketNetworkDispatcher.instance.disconnect()
    }
}
