package com.itheamc.aiassistant.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSString
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSStringDrawingUsesLineFragmentOrigin
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionCurveLinear
import platform.UIKit.boundingRectWithSize

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class PlatformToast {
    @OptIn(ExperimentalForeignApi::class)
    actual fun show(message: String, duration: Int) {
        try {
            val window = UIApplication.sharedApplication.keyWindow ?: return
            val windowWidth = window.frame.useContents { size.width }
            val windowHeight = window.frame.useContents { size.height }

            val label = UILabel().apply {
                text = message
                textColor = UIColor.whiteColor
                backgroundColor = UIColor.blackColor.colorWithAlphaComponent(0.6)
                textAlignment = NSTextAlignmentCenter
                numberOfLines = 0
                clipsToBounds = true
                sizeToFit()
            }

            val textSize = (message as NSString).boundingRectWithSize(
                size = CGSizeMake(windowWidth * 0.8, Double.MAX_VALUE),
                options = NSStringDrawingUsesLineFragmentOrigin,
                attributes = mapOf(NSFontAttributeName to label.font),
                context = null
            ).useContents { size.width to size.height }

            val labelWidth = textSize.first + 40
            val labelHeight = textSize.second + 20

            val x = (windowWidth - labelWidth) / 2
            val y = windowHeight - labelHeight - 120

            label.layer.cornerRadius = labelHeight / 2
            label.setFrame(CGRectMake(x, y, labelWidth, labelHeight))
            window.addSubview(label)

            UIView.animateWithDuration(
                duration = duration / 1000.0,
                delay = 2.0,
                options = UIViewAnimationOptionCurveLinear,
                animations = { label.alpha = 0.0 },
                completion = { _ -> label.removeFromSuperview() }
            )
        } catch (_: Exception) {
            // Do nothing here
        }
    }

    actual companion object {
        actual val LENGTH_LONG: Int = 1000
        actual val LENGTH_SHORT: Int = 400
    }
}