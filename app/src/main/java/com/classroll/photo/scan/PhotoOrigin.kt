package com.classroll.photo.scan

/**
 * 判断一张 MediaStore 图片是不是“相机拍摄的课件”。
 *
 * 只看文件名不靠谱：`IMG_` 开头的照片可能被相册移动/编辑到别的目录，截图也可能被改名。
 * 这里按优先级判断：
 *  1. 截图目录/文件名 → 永远排除
 *  2. 开关打开（包含微信/分享图片）→ 除截图外全部纳入
 *  3. DCIM/Camera/ → 相机照片
 *  4. EXIF 有 Make/Model → 相机照片（覆盖相册里移动、编辑后仍保留 EXIF 的副本）
 *  5. 相册自建目录 + IMG_yyyyMMdd_HHmmss 文件名 → 编辑后 EXIF 被剥掉的兜底
 */
object PhotoOrigin {
    private val screenshotName = Regex(
        """(screenshot|screencap|screen[-_ ]?shot|截屏|截图)""",
        RegexOption.IGNORE_CASE,
    )
    private val cameraFileName = Regex(
        """^IMG_\d{8}_\d{6}.*\.(?:jpe?g|png|heic|heif|webp)$""",
        RegexOption.IGNORE_CASE,
    )
    private val sharedFolders = listOf(
        "Pictures/WeiXin/",
        "Pictures/HeyBox/",
        "DCIM/HeyBox/",
        "Pictures/QQ/",
        "Pictures/douyin/",
        "DCIM/douyin/",
        "Download/",
        "DCIM/Alipay/",
        "DCIM/Pindd/",
        "Pictures/Pindd/",
        "Pictures/xuexitong/",
        "DCIM/Creative/",
    )

    fun isScreenshot(relativePath: String?, displayName: String): Boolean {
        val path = relativePath.orEmpty()
        if (path.startsWith("DCIM/Screenshots/", ignoreCase = true)) return true
        return screenshotName.containsMatchIn(displayName)
    }

    fun isCameraFolder(relativePath: String?): Boolean =
        relativePath.orEmpty().equals("DCIM/Camera/", ignoreCase = true)

    fun isGalleryAlbum(relativePath: String?): Boolean =
        relativePath.orEmpty().startsWith("Pictures/Gallery/", ignoreCase = true)

    fun isSharedFolder(relativePath: String?): Boolean {
        val path = relativePath.orEmpty()
        return sharedFolders.any { path.startsWith(it, ignoreCase = true) }
    }

    fun looksLikeCameraFile(displayName: String): Boolean = cameraFileName.matches(displayName)

    /** 只有不在相机目录、也不是截图/开关放行的情况下，才需要读 EXIF。 */
    fun needsExif(relativePath: String?, displayName: String, includeSharedImages: Boolean): Boolean =
        !includeSharedImages && !isScreenshot(relativePath, displayName) && !isCameraFolder(relativePath)

    fun shouldScan(
        relativePath: String?,
        displayName: String,
        hasCameraExif: Boolean,
        includeSharedImages: Boolean,
    ): Boolean {
        if (isScreenshot(relativePath, displayName)) return false
        if (includeSharedImages) return true
        if (isCameraFolder(relativePath)) return true
        if (hasCameraExif) return true
        return isGalleryAlbum(relativePath) && looksLikeCameraFile(displayName)
    }
}
