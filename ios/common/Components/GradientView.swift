//
//  GradientView.swift
//  driver
//
//  Copyright © 2019 minimal. All rights reserved.
//

import UIKit

@IBDesignable
public class GradientView: UIView {
    fileprivate var colors: [[Int]] = [[0xff3e99,0xffa35a],[0x668dff,0xff53ff],[0x6ae0d7,0x00d3ad],[0x0CB0D3,0x9F2FFF]]
    @IBInspectable var colorId: Int = 0 {
        didSet {
            redraw()
        }
    }
    @IBInspectable var startColor: UIColor = UIColor.white
    @IBInspectable var endColor: UIColor = UIColor.red
    @IBInspectable var shadowColor: UIColor?
    
    override public class var layerClass: AnyClass {
        return CAGradientLayer.self
    }
    
    override public func prepareForInterfaceBuilder() {
        super.prepareForInterfaceBuilder()
        redraw()
    }
    
    override public func layoutSubviews() {
        redraw()
    }
    
    fileprivate func redraw() {
        //self.bounds = self.frame.insetBy(dx: 4, dy: 4)
        (layer as! CAGradientLayer).startPoint = CGPoint(x: 0, y: 0)
        (layer as! CAGradientLayer).endPoint = CGPoint(x: 1, y: 1)
        let start = UIColor(hex: colors[colorId % colors.count][0])
        let end = UIColor(hex: colors[colorId % colors.count][1])
        (layer as! CAGradientLayer).shadowOffset = CGSize(width: 0, height: 0)
        (layer as! CAGradientLayer).shadowOpacity = 0.8
        (layer as! CAGradientLayer).shadowColor = shadowColor?.cgColor ?? (UIColor(hex: colors[colorId % colors.count][0])).cgColor
        (layer as! CAGradientLayer).shadowRadius = 6
        (layer as! CAGradientLayer).colors = [start.cgColor, end.cgColor]
        (layer as! CAGradientLayer).cornerRadius = 8
    }
}
