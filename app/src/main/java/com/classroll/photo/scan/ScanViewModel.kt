package com.classroll.photo.scan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.classroll.photo.data.ArchiveEntry
import com.classroll.photo.data.ArchiveIndexEntry
import com.classroll.photo.data.ArchiveIndexStore
import com.classroll.photo.data.ArchiveItem
import com.classroll.photo.data.ArchiveResult
import com.classroll.photo.data.CameraExifReader
import com.classroll.photo.data.CourseScheduleJson
import com.classroll.photo.data.CourseScheduleStore
import com.classroll.photo.data.MediaArchiveStore
import com.classroll.photo.data.MediaStorePhotoRepository
import com.classroll.photo.data.ScanSettingsStore
import com.classroll.photo.data.XlsScheduleParser
import com.classroll.photo.model.AnalysisStatus
import com.classroll.photo.model.CourseSchedule
import com.classroll.photo.model.CourseTimeMatch
import com.classroll.photo.model.PhotoGroup
import com.classroll.photo.model.PhotoItem
import com.classroll.photo.model.ScanProgress
import com.classroll.photo.model.ScanStage
import com.classroll.photo.model.TimeMatchKind
import com.classroll.photo.model.Verdict
import com.classroll.photo.recognition.GroupHashAnalyzer
import com.classroll.photo.recognition.PhotoRecognizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val scheduleStore = CourseScheduleStore(application)
    private val settingsStore = ScanSettingsStore(application)
    private val mediaRepository = MediaStorePhotoRepository(application)
    private val exifReader = CameraExifReader(application)
    private val archiveStore = MediaArchiveStore(application)
    private val archiveIndexStore = ArchiveIndexStore(application)
    private val mutableState = MutableStateFlow(ScanUiState())
    val state = mutableState.asStateFlow()
    private var scanJob: Job? = null
    private val timeFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm")

    init {
        viewModelScope.launch {
            val schedule = scheduleStore.load()
            val includeShared = settingsStore.loadIncludeSharedImages()
            mutableState.update { it.copy(schedule = schedule, includeSharedImages = includeShared) }
        }
        refreshLibrary()
    }

    fun startPicker(uris: List<Uri>) {
        if (uris.isEmpty()) return
        startJob { startedAt ->
            mutableState.update {
                it.copy(
                    source = ScanSource.Picker,
                    groups = mapOf(PICKER_GROUP_ID to pickerGroup()),
                    photos = emptyList(),
                    selectedIds = emptySet(),
                    filter = Verdict.Likely,
                    progress = ScanProgress(ScanStage.Querying, uris.size, 0, "读取所选照片", 0L),
                    notice = null,
                    confirmed = false,
                    archiving = false,
                    archiveDone = 0,
                    archiveTotal = 0,
                    archive = null,
                )
            }
            val selected = mediaRepository.selectedPhotoUris(uris)
            if (selected.isEmpty()) error("没有读取到可用照片，请重新选择")
            val photos = selected.mapIndexed { index, media ->
                PhotoItem(
                    id = "picker-${media.id}-$index",
                    imageUri = media.uri,
                    displayName = media.displayName,
                    mediaStoreId = media.id.takeIf { it >= 0 },
                    groupId = PICKER_GROUP_ID,
                    title = media.displayName.substringBeforeLast('.'),
                    timeLabel = timeFormat.format(media.takenAt ?: media.addedAt),
                    takenAt = media.takenAt ?: media.addedAt,
                    timeSource = media.timeSource,
                )
            }
            recognize(photos, mapOf(PICKER_GROUP_ID to pickerGroup()), startedAt)
        }
    }

    fun startFullScan(includeArchived: Boolean = false) {
        val schedule = mutableState.value.schedule
        if (schedule == null) {
            mutableState.update { it.copy(notice = "请先在设置里导入课程表 JSON") }
            return
        }
        val context = getApplication<Application>()
        if (!MediaPermission.hasFullAccess(context)) {
            mutableState.update { it.copy(notice = "需要“全部照片”权限才能扫描完整相册；也可以手动选择照片识别") }
            return
        }
        startJob { startedAt ->
            mutableState.update {
                it.copy(
                    source = ScanSource.FullAlbum,
                    photos = emptyList(),
                    groups = emptyMap(),
                    selectedIds = emptySet(),
                    filter = Verdict.Likely,
                    progress = ScanProgress(ScanStage.Querying, 0, 0, "读取相册索引", 0L),
                    notice = null,
                    confirmed = false,
                    archiving = false,
                    archiveDone = 0,
                    archiveTotal = 0,
                    archive = null,
                )
            }
            val now = LocalDateTime.now()
            val includeShared = mutableState.value.includeSharedImages
            val archivedIds = if (includeArchived) emptySet() else archiveIndexStore.archivedSourceIds()
            val mediaPhotos = mediaRepository.query(schedule, now)
            val sessions = schedule.sessions(schedule.semesterStart, now.toLocalDate())
            val groups = PhotoTimeMatcher.buildGroups(sessions)
            var timeMatched = 0
            var archivedExcluded = 0
            val photos = withContext(Dispatchers.IO) {
                mediaPhotos.mapNotNull { media ->
                    if (media.id in archivedIds) {
                        archivedExcluded++
                        return@mapNotNull null
                    }
                    val time = media.takenAt ?: media.addedAt
                    val match = PhotoTimeMatcher.matchPhoto(time, sessions) ?: return@mapNotNull null
                    timeMatched++
                    val hasCameraExif = PhotoOrigin.needsExif(media.relativePath, media.displayName, includeShared) &&
                        exifReader.hasCameraExif(media.uri)
                    if (!PhotoOrigin.shouldScan(media.relativePath, media.displayName, hasCameraExif, includeShared)) {
                        return@mapNotNull null
                    }
                    val boundary = match.kind != TimeMatchKind.Core
                    val groupId = if (boundary) "${match.session.id}:boundary" else match.session.id
                    PhotoItem(
                        id = "media-${media.id}",
                        imageUri = media.uri,
                        displayName = media.displayName,
                        mediaStoreId = media.id,
                        groupId = groupId,
                        title = media.displayName.substringBeforeLast('.'),
                        timeLabel = timeFormat.format(time) + if (media.timeSource == com.classroll.photo.model.PhotoTimeSource.Added) "（加入时间）" else "",
                        takenAt = time,
                        timeSource = media.timeSource,
                        verdict = if (boundary) Verdict.Boundary else Verdict.Doubtful,
                        relativePath = media.relativePath,
                    )
                }
            }
            val excluded = timeMatched - photos.size
            mutableState.update {
                it.copy(
                    groups = groups,
                    photos = photos,
                    progress = ScanProgress(
                        ScanStage.Matching,
                        photos.size,
                        photos.size,
                        "课程时间匹配完成：${photos.size} 张相机照片" +
                            (if (excluded > 0) "（排除 $excluded 张截图/非相机图片）" else "") +
                            (if (archivedExcluded > 0) " · 跳过 $archivedExcluded 张已归档原图" else ""),
                        android.os.SystemClock.elapsedRealtime() - startedAt,
                    ),
                )
            }
            recognize(photos, groups, startedAt)
        }
    }

    private fun startJob(block: suspend (Long) -> Unit) {
        scanJob?.cancel()
        val startedAt = android.os.SystemClock.elapsedRealtime()
        scanJob = viewModelScope.launch {
            try {
                block(startedAt)
            } catch (error: CancellationException) {
                mutableState.update { it.copy(progress = it.progress.copy(stage = ScanStage.Cancelled, message = "已取消扫描")) }
                throw error
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(
                        progress = it.progress.copy(stage = ScanStage.Error, message = error.message ?: "扫描失败"),
                        notice = error.message ?: "扫描失败",
                    )
                }
            }
        }
    }

    private suspend fun recognize(initial: List<PhotoItem>, groups: Map<String, PhotoGroup>, elapsed: Long) {
        if (initial.isEmpty()) {
            mutableState.update {
                it.copy(
                    selectedIds = emptySet(),
                    progress = ScanProgress(ScanStage.Done, 0, 0, "没有找到落在课程时间内的照片", elapsed),
                )
            }
            return
        }
        PhotoRecognizer(getApplication()).use { recognizer ->
            val coarse = mutableListOf<PhotoItem>()
            initial.forEachIndexed { index, photo ->
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val boundary = groups[photo.groupId]?.boundary == true
                val analyzed = recognizer.analyzeCoarse(photo, boundary)
                coarse += analyzed
                mutableState.update {
                    it.copy(
                        photos = coarse.toList(),
                        progress = ScanProgress(
                            ScanStage.Coarse,
                            initial.size,
                            index + 1,
                            "粗筛 ${index + 1}/${initial.size}",
                            android.os.SystemClock.elapsedRealtime() - elapsed,
                        ),
                    )
                }
            }
            val enriched = GroupHashAnalyzer.enrich(coarse).sortedBy { it.takenAt }
            mutableState.update { it.copy(photos = enriched) }
            val ocrCandidates = enriched.filter { it.status == AnalysisStatus.CoarseDone }.sortedByDescending { it.coarse.screenScore }
            val afterOcr = enriched.associateBy { it.id }.toMutableMap()
            ocrCandidates.forEachIndexed { index, photo ->
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val boundary = groups[photo.groupId]?.boundary == true
                val analyzed = recognizer.analyzeOcr(photo, boundary)
                afterOcr[photo.id] = analyzed
                val current = afterOcr.values.sortedBy { it.takenAt }
                mutableState.update {
                    it.copy(
                        photos = current,
                        progress = ScanProgress(
                            ScanStage.Ocr,
                            ocrCandidates.size,
                            index + 1,
                            "中文文字识别 ${index + 1}/${ocrCandidates.size}",
                            android.os.SystemClock.elapsedRealtime() - elapsed,
                        ),
                    )
                }
            }
            val finalPhotos = afterOcr.values.sortedBy { it.takenAt }
            val ocrDone = finalPhotos.count { it.status == AnalysisStatus.OcrDone }
            val skipped = finalPhotos.count { it.status == AnalysisStatus.OcrSkipped }
            val failed = finalPhotos.count { it.status == AnalysisStatus.Failed }
            mutableState.update {
                it.copy(
                    photos = finalPhotos,
                    selectedIds = finalPhotos.filter { photo -> photo.verdict == Verdict.Likely }.map { it.id }.toSet(),
                    progress = ScanProgress(
                        ScanStage.Done,
                        finalPhotos.size,
                        finalPhotos.size,
                        "识别完成：${finalPhotos.size} 张 · 文字识别 $ocrDone · 跳过 $skipped · 失败 $failed",
                        android.os.SystemClock.elapsedRealtime() - elapsed,
                    ),
                )
            }
        }
    }

    fun confirmSelection() {
        mutableState.update { it.copy(confirmed = true) }
    }

    fun cancelScan() {
        scanJob?.cancel()
    }

    fun clearScan() {
        scanJob?.cancel()
        mutableState.update {
            it.copy(
                photos = emptyList(),
                groups = emptyMap(),
                selectedIds = emptySet(),
                source = null,
                progress = ScanProgress(),
                notice = null,
                confirmed = false,
            )
        }
    }

    fun setFilter(filter: Verdict) {
        mutableState.update { it.copy(filter = filter) }
    }

    fun togglePhoto(id: String) {
        mutableState.update {
            it.copy(selectedIds = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id)
        }
    }

    fun selectCurrentFilter() {
        val ids = mutableState.value.let { state -> state.photos.filter { it.verdict == state.filter }.map { it.id }.toSet() }
        mutableState.update { state ->
            state.copy(selectedIds = state.selectedIds + ids, notice = "已全选本档照片")
        }
    }

    fun invertCurrentFilter() {
        val ids = mutableState.value.let { state -> state.photos.filter { it.verdict == state.filter }.map { it.id }.toSet() }
        mutableState.update { state ->
            state.copy(
                selectedIds = state.selectedIds.union(ids) - state.selectedIds.intersect(ids),
                notice = "已反选本档照片",
            )
        }
    }

    fun skipGroup(groupId: String) {
        val ids = mutableState.value.let { state ->
            state.photos.filter { it.groupId == groupId && it.verdict == state.filter }.map { it.id }.toSet()
        }
        mutableState.update { state ->
            state.copy(selectedIds = state.selectedIds - ids, notice = "已排除本组当前档照片")
        }
    }

    fun retryPhoto(id: String) {
        val photo = mutableState.value.photos.firstOrNull { it.id == id } ?: return
        val boundary = mutableState.value.groups[photo.groupId]?.boundary == true
        viewModelScope.launch {
            PhotoRecognizer(getApplication()).use { recognizer ->
                val retried = if (photo.status == AnalysisStatus.CoarseDone || photo.status == AnalysisStatus.OcrSkipped) {
                    recognizer.analyzeOcr(photo, boundary, force = photo.status == AnalysisStatus.OcrSkipped)
                } else {
                    recognizer.analyzeCoarse(photo, boundary)
                }
                mutableState.update { state ->
                    state.copy(
                        photos = state.photos.map { if (it.id == id) retried else it },
                        notice = if (retried.error == null) "已重新识别这张照片" else retried.error,
                    )
                }
            }
        }
    }

    fun importSchedule(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val context = getApplication<Application>()
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("无法读取所选文件")
                    if (bytes.size > 6_000_000) throw IllegalArgumentException("文件超过 6 MB，请检查文件内容")
                    parseScheduleFile(bytes, mutableState.value.schedule)
                }
            }
            result.fold(
                onSuccess = { schedule ->
                    mutableState.update {
                        it.copy(
                            pendingSchedule = schedule,
                            notice = "已读取 ${schedule.courses.size} 条课程，预览确认后保存",
                        )
                    }
                },
                onFailure = { error -> mutableState.update { it.copy(notice = error.message ?: "课程表导入失败") } },
            )
        }
    }

    /** 从设置进入当前课表的预览编辑页；返回放弃，保存才写回。 */
    fun beginScheduleEditing() {
        val schedule = mutableState.value.schedule ?: return
        mutableState.update { it.copy(pendingSchedule = schedule, notice = null) }
    }

    private fun parseScheduleFile(bytes: ByteArray, base: CourseSchedule?): CourseSchedule {
        val isOle = bytes.size >= 4 &&
            bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte() &&
            bytes[2] == 0x11.toByte() && bytes[3] == 0xE0.toByte()
        val isZip = bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
        return when {
            isOle -> XlsScheduleParser.parse(bytes, base)
            isZip -> throw IllegalArgumentException("暂不支持 .xlsx，请在电脑上另存为 .xls 或 JSON")
            else -> CourseScheduleJson.parseSchedule(bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF"))
        }
    }

    /** 预览页点保存后才真正替换当前课表。 */
    fun savePendingSchedule(schedule: CourseSchedule) {
        viewModelScope.launch {
            scheduleStore.save(schedule)
            mutableState.update {
                it.copy(
                    schedule = schedule,
                    pendingSchedule = null,
                    notice = "课表已保存：${schedule.uniqueCourseCount} 门课程",
                )
            }
        }
    }

    fun discardPendingSchedule() {
        mutableState.update { it.copy(pendingSchedule = null, notice = "已放弃这次导入，原课表不变") }
    }

    fun clearSchedule() {
        viewModelScope.launch {
            scheduleStore.clear()
            mutableState.update {
                it.copy(schedule = null, pendingSchedule = null, notice = "课表配置已删除")
            }
        }
    }

    /** 保存手工编辑过的作息，立即写回 DataStore。 */
    fun updateSchedule(schedule: CourseSchedule) {
        viewModelScope.launch {
            scheduleStore.save(schedule)
            mutableState.update { it.copy(schedule = schedule, notice = "作息已保存") }
        }
    }

    /** 把勾选的照片写入归档索引；不复制、不移动原图。 */
    fun archiveSelection() {
        val snapshot = mutableState.value
        val schedule = snapshot.schedule ?: run {
            mutableState.update { it.copy(notice = "还没有导入课程表，无法归档") }
            return
        }
        if (snapshot.archiving) return
        if (snapshot.source != ScanSource.FullAlbum) {
            mutableState.update { it.copy(notice = "手动选择的照片不归档；请用全相册扫描按课程归档") }
            return
        }
        val selected = snapshot.photos.filter { it.id in snapshot.selectedIds }
        if (selected.isEmpty()) {
            mutableState.update { it.copy(notice = "还没有勾选任何照片") }
            return
        }
        val now = LocalDateTime.now()
        val entries = selected.mapNotNull { photo ->
            val group = snapshot.groups[photo.groupId] ?: return@mapNotNull null
            val sourceId = photo.mediaStoreId ?: return@mapNotNull null
            val taken = photo.takenAt ?: group.date
            ArchiveIndexEntry(
                sourceId = sourceId,
                uri = photo.imageUri,
                displayName = photo.displayName,
                term = schedule.archiveTerm,
                course = group.course,
                week = group.week ?: 0,
                date = taken?.toLocalDate()?.toString() ?: "",
                sessionLabel = group.session,
                addedAt = now.toString(),
            )
        }
        if (entries.isEmpty()) {
            mutableState.update { it.copy(notice = "选中的照片没有可归档的原图") }
            return
        }
        mutableState.update {
            it.copy(archiving = true, archive = null, archiveDone = 0, archiveTotal = entries.size, notice = null)
        }
        viewModelScope.launch {
            val result = runCatching { archiveIndexStore.addAll(entries) }
            result.fold(
                onSuccess = {
                    val value = ArchiveResult(
                        term = schedule.archiveTerm,
                        copied = entries.size,
                        skipped = 0,
                        failed = selected.size - entries.size,
                        albums = entries.groupingBy { it.course }.eachCount(),
                        entries = entries.map { entry ->
                            ArchiveEntry(
                                course = entry.course,
                                fileName = entry.displayName,
                                sourceName = entry.displayName,
                                copied = true,
                                skipped = false,
                                relativePath = "",
                                sourceId = entry.sourceId,
                            )
                        },
                    )
                    mutableState.update {
                        it.copy(
                            archiving = false,
                            archiveDone = entries.size,
                            archive = value,
                            notice = "已归档 ${entries.size} 张，原图未复制",
                        )
                    }
                    refreshLibrary()
                },
                onFailure = { error ->
                    mutableState.update { it.copy(archiving = false, notice = "归档失败：${error.message ?: "未知错误"}") }
                },
            )
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            val entries = runCatching { archiveIndexStore.load() }.getOrDefault(emptyList())
            val alive = runCatching { archiveStore.aliveImageIds() }.getOrDefault(emptySet())
            val items = entries.map { entry ->
                ArchiveItem(
                    term = entry.term,
                    course = entry.course,
                    week = entry.week,
                    date = entry.date.takeIf { it.isNotBlank() }?.let { value ->
                        runCatching { LocalDate.parse(value) }.getOrNull()
                    },
                    uri = entry.uri,
                    fileName = entry.displayName,
                    available = entry.sourceId in alive,
                    sessionLabel = entry.sessionLabel,
                )
            }
            val albums = items
                .groupBy { it.term to it.course }
                .map { (key, list) ->
                    com.classroll.photo.data.ArchiveAlbum(
                        term = key.first,
                        course = key.second,
                        count = list.size,
                        coverUri = list.firstOrNull { it.available }?.uri ?: list.firstOrNull()?.uri,
                        relativePath = "",
                    )
                }
                .sortedWith(compareByDescending<com.classroll.photo.data.ArchiveAlbum> { it.term }.thenBy { it.course })
            mutableState.update { it.copy(archiveItems = items, libraryAlbums = albums) }
        }
    }

    /** 只从索引里移除某门课，原图不动；这些原图会在下一次扫描重新出现。 */
    fun deleteArchiveCourse(term: String, course: String) {
        viewModelScope.launch {
            archiveIndexStore.removeCourse(term, course)
            refreshLibrary()
            mutableState.update { it.copy(notice = "已从归档移除「$course」，原图不受影响") }
        }
    }

    fun exportIndex(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val json = archiveIndexStore.exportJson()
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("无法写入文件")
            }
            result.fold(
                onSuccess = { mutableState.update { it.copy(notice = "索引已导出") } },
                onFailure = { error -> mutableState.update { it.copy(notice = "导出失败：${error.message ?: "未知错误"}") } },
            )
        }
    }

    fun openArchiveAlbum(term: String, course: String) {
        mutableState.update { it.copy(openAlbum = term to course, albumPhotos = emptyList()) }
        viewModelScope.launch {
            val photos = runCatching { archiveStore.loadAlbumPhotos(term, course) }.getOrDefault(emptyList())
            mutableState.update { it.copy(albumPhotos = photos) }
        }
    }

    fun closeArchiveAlbum() {
        mutableState.update { it.copy(openAlbum = null, albumPhotos = emptyList()) }
    }

    fun consumeNotice() {
        mutableState.update { it.copy(notice = null) }
    }

    /** 只影响下一次全相册扫描；选图流程始终按用户明确选择的照片处理。 */
    fun setIncludeSharedImages(enabled: Boolean) {
        mutableState.update {
            it.copy(
                includeSharedImages = enabled,
                notice = if (enabled) {
                    "下次扫描会包含微信/QQ/分享图片"
                } else {
                    "下次扫描只扫描相机照片"
                },
            )
        }
        viewModelScope.launch { settingsStore.saveIncludeSharedImages(enabled) }
    }

    private fun pickerGroup() = PhotoGroup(
        id = PICKER_GROUP_ID,
        course = "手动选择的照片",
        session = "手动选择",
        meta = "不参与课程时间匹配 · 全部在本机识别",
        week = null,
        date = null,
    )

    private companion object {
        const val PICKER_GROUP_ID = "picker-manual"
    }
}
