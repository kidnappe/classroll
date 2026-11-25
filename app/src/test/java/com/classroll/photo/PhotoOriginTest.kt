package com.classroll.photo

import com.classroll.photo.scan.PhotoOrigin
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoOriginTest {
    @Test
    fun screenshotsAreAlwaysExcluded() {
        assertFalse(PhotoOrigin.shouldScan("DCIM/Screenshots/", "Screenshot_2026-11-25-09-00-00_com.mm.jpg", false, false))
        assertFalse(PhotoOrigin.shouldScan("DCIM/Screenshots/", "Screenshot_2026-11-25-09-00-00_com.mm.jpg", true, true))
        assertFalse(PhotoOrigin.shouldScan("Pictures/WeiXin/", "Screenshot_2026-11-25-09-00-00.jpg", true, true))
    }

    @Test
    fun cameraFolderIsIncludedWithoutExif() {
        assertTrue(PhotoOrigin.shouldScan("DCIM/Camera/", "IMG_20260923_171250.jpg", false, false))
    }

    @Test
    fun editedGalleryPhotoIsIncludedByExif() {
        assertTrue(
            PhotoOrigin.shouldScan(
                "Pictures/Gallery/owner/课堂要求/",
                "IMG_20260909_173948_1789110444433edit.jpg",
                hasCameraExif = true,
                includeSharedImages = false,
            ),
        )
    }

    @Test
    fun galleryCameraNameIsFallbackWhenExifIsStripped() {
        assertTrue(
            PhotoOrigin.shouldScan(
                "Pictures/Gallery/owner/课堂要求/",
                "IMG_20260910_101225.jpg",
                hasCameraExif = false,
                includeSharedImages = false,
            ),
        )
    }

    @Test
    fun galleryNonCameraNameWithoutExifIsExcluded() {
        assertFalse(
            PhotoOrigin.shouldScan(
                "Pictures/Gallery/owner/课堂要求/",
                "mmexport1789382506628_1789382539538edit.jpg",
                hasCameraExif = false,
                includeSharedImages = false,
            ),
        )
    }

    @Test
    fun wechatImageIsExcludedByDefaultAndIncludedWithSwitch() {
        assertFalse(PhotoOrigin.shouldScan("Pictures/WeiXin/", "mmexport1795420013615.jpg", false, false))
        assertTrue(PhotoOrigin.shouldScan("Pictures/WeiXin/", "mmexport1795420013615.jpg", false, true))
    }

    @Test
    fun wechatImageWithCameraExifIsIncludedEvenByDefault() {
        assertTrue(PhotoOrigin.shouldScan("Pictures/WeiXin/", "mmexport1795420013615.jpg", true, false))
    }

    @Test
    fun exifIsOnlyNeededOutsideCameraAndScreenshots() {
        assertFalse(PhotoOrigin.needsExif("DCIM/Camera/", "IMG_1.jpg", false))
        assertFalse(PhotoOrigin.needsExif("DCIM/Screenshots/", "Screenshot_1.jpg", false))
        assertFalse(PhotoOrigin.needsExif("Pictures/WeiXin/", "mmexport1.jpg", true))
        assertTrue(PhotoOrigin.needsExif("Pictures/WeiXin/", "mmexport1.jpg", false))
        assertTrue(PhotoOrigin.needsExif("Pictures/Gallery/owner/课堂要求/", "IMG_20260910_101225.jpg", false))
    }
}
