package com.classroll.photo.scan

import com.classroll.photo.data.ArchiveAlbum
import com.classroll.photo.data.ArchiveItem
import com.classroll.photo.data.ArchivePhoto
import com.classroll.photo.data.ArchiveResult
import com.classroll.photo.model.CourseSchedule
import com.classroll.photo.model.PhotoGroup
import com.classroll.photo.model.PhotoItem
import com.classroll.photo.model.ScanProgress
import com.classroll.photo.model.Verdict

enum class ScanSource {
    FullAlbum,
    Picker,
}

data class ScanUiState(
    val schedule: CourseSchedule? = null,
    /** 刚导入、还没保存的课表；不为空时显示预览编辑页。 */
    val pendingSchedule: CourseSchedule? = null,
    val progress: ScanProgress = ScanProgress(),
    val photos: List<PhotoItem> = emptyList(),
    val groups: Map<String, PhotoGroup> = emptyMap(),
    val selectedIds: Set<String> = emptySet(),
    val filter: Verdict = Verdict.Likely,
    val source: ScanSource? = null,
    val notice: String? = null,
    /** 是否已经点过“完成”。 */
    val confirmed: Boolean = false,
    /** 打开后微信/QQ/分享目录的图片也参与识别；默认只扫描相机照片。 */
    val includeSharedImages: Boolean = false,
    /** 正在复制到系统相册。 */
    val archiving: Boolean = false,
    val archiveDone: Int = 0,
    val archiveTotal: Int = 0,
    /** 本次归档结果；为空表示还没归档过。 */
    val archive: ArchiveResult? = null,
    /** 已归档的课程相册，来自 Pictures/课程归档/<学期>/<课程>/。 */
    val libraryAlbums: List<ArchiveAlbum> = emptyList(),
    /** 归档库全部照片，带学期/课程/教学周/日期，用于按周或按课程分组。 */
    val archiveItems: List<ArchiveItem> = emptyList(),
    val albumPhotos: List<ArchivePhoto> = emptyList(),
    val openAlbum: Pair<String, String>? = null,
) {
    val counts: Map<Verdict, Int>
        get() = photos.groupingBy { it.verdict }.eachCount()

    val done: Boolean
        get() = progress.stage == com.classroll.photo.model.ScanStage.Done

    val libraryTotal: Int
        get() = archiveItems.size
}
