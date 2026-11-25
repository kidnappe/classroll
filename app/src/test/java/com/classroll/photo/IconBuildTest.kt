package com.classroll.photo

import com.classroll.photo.ui.ClassrollIcons
import org.junit.Assert.assertNotNull
import org.junit.Test

class IconBuildTest {
    @Test
    fun `所有手绘图标都能构建`() {
        assertNotNull(ClassrollIcons.Github)
        assertNotNull(ClassrollIcons.Code)
        assertNotNull(ClassrollIcons.AddPhoto)
        assertNotNull(ClassrollIcons.Scan)
    }
}
