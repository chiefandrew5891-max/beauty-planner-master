@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.andrey.beautyplanner.notifications

import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSFileManager
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UniformTypeIdentifiers.UTTypeAudio
import platform.darwin.NSObject

actual object NotificationSoundPicker {
    actual fun pick(onPicked: (String, String) -> Unit, onError: (String) -> Unit) {
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        while (top.presentedViewController != null) top = top.presentedViewController!!

        val delegate = SoundPickerDelegate(onPicked, onError)
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeAudio),
            asCopy = true
        )
        picker.delegate = delegate
        SoundPickerHolder.current = delegate
        top.presentViewController(picker, animated = true, completion = null)
    }
}

private class SoundPickerDelegate(
    val onPicked: (String, String) -> Unit,
    val onError: (String) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        SoundPickerHolder.current = null
        val src = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return onError("read")
        val displayName = src.lastPathComponent ?: "custom"
        val ext = displayName.substringAfterLast('.', "").lowercase()
        if (ext !in setOf("wav", "aif", "aiff", "caf")) return onError("format")

        val fm = NSFileManager.defaultManager
        val lib = NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, true).first() as String
        val dir = "$lib/Sounds"
        fm.createDirectoryAtPath(dir, true, null, null)
        (fm.contentsOfDirectoryAtPath(dir, null) as? List<*>)
            ?.filter { (it as String).startsWith("custom_") }
            ?.forEach { fm.removeItemAtPath("$dir/$it", null) }

        val soundId = "custom_${NSDate().timeIntervalSince1970.toLong()}.$ext"
        val dstPath = "$dir/$soundId"
        if (!fm.copyItemAtURL(src, NSURL.fileURLWithPath(dstPath), null)) return onError("read")

        val player = AVAudioPlayer(contentsOfURL = NSURL.fileURLWithPath(dstPath), error = null)
        val duration = player?.duration ?: 0.0
        if (player == null || duration <= 0.0 || duration > MAX_CUSTOM_SOUND_SECONDS) {
            fm.removeItemAtPath(dstPath, null)
            return onError("duration")
        }
        onPicked(soundId, displayName)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        SoundPickerHolder.current = null
    }
}

private object SoundPickerHolder { var current: Any? = null }