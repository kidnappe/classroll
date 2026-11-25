package com.classroll.photo.data

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/**
 * 读相机 EXIF。相册里移动、裁剪、编辑过的副本通常会保留 Make/Model，
 * 因此比“是否在 DCIM/Camera 目录”更可靠；微信/QQ 二次保存的图片一般会被剥掉 EXIF。
 */
class CameraExifReader(context: Context) {
    private val appContext = context.applicationContext

    fun hasCameraExif(uri: String): Boolean = runCatching {
        appContext.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
            val exif = ExifInterface(stream)
            val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
            make.isNotEmpty() || model.isNotEmpty()
        } ?: false
    }.getOrDefault(false)
}
