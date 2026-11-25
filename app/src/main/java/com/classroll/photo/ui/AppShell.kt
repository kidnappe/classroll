package com.classroll.photo.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classroll.photo.BuildConfig
import com.classroll.photo.data.ArchiveItem
import com.classroll.photo.model.PhotoItem
import com.classroll.photo.scan.MediaPermission
import com.classroll.photo.scan.ScanSource
import com.classroll.photo.scan.ScanViewModel
import com.classroll.photo.ui.screens.ArchiveManagerScreen
import com.classroll.photo.ui.screens.CandidateFullscreenScreen
import com.classroll.photo.ui.screens.ConfirmScreen
import com.classroll.photo.ui.screens.CoursesScreen
import com.classroll.photo.ui.screens.FullscreenPhotoScreen
import com.classroll.photo.ui.screens.LibraryScreen
import com.classroll.photo.ui.screens.QuickScanMode
import com.classroll.photo.ui.screens.ResultScreen
import com.classroll.photo.ui.screens.SchedulePreviewScreen
import com.classroll.photo.ui.screens.ScanScreen
import com.classroll.photo.ui.screens.SettingsScreen
import com.classroll.photo.ui.screens.TimetableScreen
import com.classroll.photo.ui.theme.LocalSkin
import com.classroll.photo.ui.theme.corner
import com.classroll.photo.ui.theme.PaperBackground
import com.classroll.photo.ui.theme.PaperCard
import com.classroll.photo.ui.theme.PaperGood
import com.classroll.photo.ui.theme.PaperHighlight
import com.classroll.photo.ui.theme.PaperInk
import com.classroll.photo.ui.theme.PaperLine
import com.classroll.photo.ui.theme.PaperMuted
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppTab { Courses, Library, Settings }

private enum class FlowStep { Scan, Confirm, Result }

private data class PhotoViewer(val photos: List<ArchiveItem>, val index: Int)

private data class CandidateViewer(val photos: List<PhotoItem>, val index: Int)

@Composable
fun AppShell(
    skinId: String,
    onSkinChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val viewModel: ScanViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(AppTab.Courses) }
    var flowStep by remember { mutableStateOf<FlowStep?>(null) }
    var flowOrigin by remember { mutableStateOf(AppTab.Courses) }
    var showTimetable by remember { mutableStateOf(false) }
    var showArchiveManager by remember { mutableStateOf(false) }
    var pendingIncludeArchived by remember { mutableStateOf(false) }
    var pendingSessionIds by remember { mutableStateOf<Set<String>?>(null) }
    var lastScanSessionIds by remember { mutableStateOf<Set<String>?>(null) }
    var photoViewer by remember { mutableStateOf<PhotoViewer?>(null) }
    var candidateViewer by remember { mutableStateOf<CandidateViewer?>(null) }
    var hasFullAccess by remember { mutableStateOf(MediaPermission.hasFullAccess(context)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var noticeJob by remember { mutableStateOf<Job?>(null) }
    val notify: (String) -> Unit = { message ->
        noticeJob?.cancel()
        noticeJob = scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            launch { snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Indefinite) }
            delay(1600)
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasFullAccess = MediaPermission.hasFullAccess(context)
        if (hasFullAccess) {
            viewModel.startFullScan(pendingIncludeArchived, pendingSessionIds)
        } else {
            notify("没有获得全部照片权限；可以改用手动选择照片识别")
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importSchedule(uri)
    }
    fun openScheduleFilePicker() {
        fileLauncher.launch(
            arrayOf(
                "application/json",
                "application/vnd.ms-excel",
                "application/octet-stream",
                "text/*",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ),
        )
    }
    fun openSchedulePage() {
        if (state.schedule != null) viewModel.beginScheduleEditing() else openScheduleFilePicker()
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportIndex(uri)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasFullAccess = MediaPermission.hasFullAccess(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.notice) {
        state.notice?.let {
            notify(it)
            viewModel.consumeNotice()
        }
    }

    fun openScanFlow() {
        flowOrigin = currentTab
        flowStep = when {
            state.progress.running -> FlowStep.Scan
            state.archive != null -> FlowStep.Result
            state.done && state.photos.isNotEmpty() -> FlowStep.Confirm
            else -> FlowStep.Scan
        }
    }

    fun startFullScan(includeArchived: Boolean = false, sessionIds: Set<String>? = null) {
        pendingIncludeArchived = includeArchived
        pendingSessionIds = sessionIds
        lastScanSessionIds = sessionIds
        flowStep = FlowStep.Scan
        flowOrigin = currentTab
        when {
            state.schedule == null -> {
                notify("请先导入课程表")
                openScheduleFilePicker()
            }
            !hasFullAccess -> permissionLauncher.launch(MediaPermission.requiredPermissions())
            else -> viewModel.startFullScan(includeArchived, sessionIds)
        }
    }

    fun openPermissionSettings() {
        val partial = android.os.Build.VERSION.SDK_INT >= 34 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (partial && !hasFullAccess) {
            context.startActivity(
                android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:${context.packageName}"),
                ),
            )
        } else {
            permissionLauncher.launch(MediaPermission.requiredPermissions())
        }
    }

    fun exitFlow() {
        if (state.progress.running) viewModel.cancelScan()
        flowStep = null
        currentTab = flowOrigin
    }

    val pendingSchedule = state.pendingSchedule
    BackHandler(enabled = pendingSchedule == null && (flowStep != null || showTimetable || showArchiveManager || currentTab == AppTab.Library)) {
        when {
            showArchiveManager -> showArchiveManager = false
            showTimetable -> showTimetable = false
            flowStep != null -> exitFlow()
            currentTab == AppTab.Library -> currentTab = AppTab.Courses
        }
    }

    val quickMode = when {
        state.progress.running -> QuickScanMode.Scanning
        state.done && state.photos.isNotEmpty() -> QuickScanMode.Rescan
        else -> QuickScanMode.Scan
    }
    Scaffold(
        containerColor = PaperBackground,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val skin = LocalSkin.current
                Snackbar(
                    snackbarData = data,
                    containerColor = skin.snackbarContainer,
                    contentColor = skin.snackbarContent,
                    shape = MaterialTheme.shapes.small,
                )
            }
        },
        bottomBar = {
            if (photoViewer == null && candidateViewer == null && pendingSchedule == null && !showTimetable && !showArchiveManager) {
                BottomNavigation(
                    selectedTab = currentTab,
                    scanning = flowStep != null,
                    onTab = { tab ->
                        if (flowStep != null) exitFlow()
                        currentTab = tab
                    },
                    onScan = ::openScanFlow,
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).paperGrain()) {
            val currentFlowStep = flowStep
            val schedule = state.schedule
            if (pendingSchedule != null) {
                SchedulePreviewScreen(
                    initial = pendingSchedule,
                    onSave = { schedule ->
                        viewModel.savePendingSchedule(schedule)
                        currentTab = AppTab.Courses
                    },
                    onDiscard = { viewModel.discardPendingSchedule() },
                    onImportFile = { openScheduleFilePicker() },
                    onDeleteSchedule = {
                        viewModel.clearSchedule()
                        currentTab = AppTab.Courses
                    },
                )
            } else if (showArchiveManager) {
                ArchiveManagerScreen(
                    albums = state.libraryAlbums,
                    onBack = { showArchiveManager = false },
                    onDelete = { term, course -> viewModel.deleteArchiveCourse(term, course) },
                    onExport = { exportLauncher.launch("classroll-archive-index.json") },
                )
            } else if (showTimetable && schedule != null) {
                TimetableScreen(
                    schedule = schedule,
                    onBack = { showTimetable = false },
                    onSave = viewModel::updateSchedule,
                )
            } else if (currentFlowStep == null) {
                when (currentTab) {
                    AppTab.Courses -> CoursesScreen(
                        schedule = state.schedule,
                        quickMode = quickMode,
                        selectedCount = state.selectedIds.size,
                        candidateCount = state.photos.size,
                        onQuickScan = ::openScanFlow,
                        onScanSession = { sessionId -> startFullScan(sessionIds = setOf(sessionId)) },
                        onScanToday = { sessionIds -> startFullScan(sessionIds = sessionIds) },
                        onImport = { openScheduleFilePicker() },
                    )
                    AppTab.Library -> LibraryScreen(
                        items = state.archiveItems,
                        added = state.archive?.copied ?: 0,
                        onScan = ::openScanFlow,
                        onOpenPhotos = { photos, index ->
                            val clicked = photos.getOrNull(index)
                            val all = state.archiveItems
                            val start = all.indexOfFirst { it.uri == clicked?.uri }.takeIf { it >= 0 } ?: 0
                            photoViewer = PhotoViewer(if (all.isEmpty()) photos else all, start)
                        },
                        onRemovePhotos = viewModel::deleteArchivedItems,
                    )
                    AppTab.Settings -> SettingsScreen(
                        schedule = state.schedule,
                        permissionLabel = if (hasFullAccess) "全部照片" else "未授权",
                        includeSharedImages = state.includeSharedImages,
                        onImport = { openScheduleFilePicker() },
                        onOpenSchedule = { openSchedulePage() },
                        onPermission = ::openPermissionSettings,
                        onToggleIncludeShared = viewModel::setIncludeSharedImages,
                        onTimetable = { showTimetable = true },
                        archiveCount = state.libraryAlbums.size,
                        onManageArchive = { showArchiveManager = true },
                        onSaveSchedule = viewModel::updateSchedule,
                        skinId = skinId,
                        onSkinChange = onSkinChange,
                        onAbout = { notify("课页 ${BuildConfig.VERSION_NAME} · 所有识别都在本机完成，归档只复制、不移动、不删除原图") },
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (currentFlowStep != FlowStep.Scan) {
                        FlowHeader(step = currentFlowStep, onBack = ::exitFlow)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        when (currentFlowStep) {
                            FlowStep.Scan -> ScanScreen(
                                progress = state.progress,
                                counts = state.counts,
                                hasSchedule = state.schedule != null,
                                onStartFullScan = { startFullScan() },
                                onRescanAll = { startFullScan(true) },
                                onCancel = viewModel::cancelScan,
                                onRescan = { startFullScan(sessionIds = lastScanSessionIds) },
                                onViewResults = { flowStep = FlowStep.Confirm },
                            )
                            FlowStep.Confirm -> ConfirmScreen(
                                photos = state.photos,
                                groups = state.groups,
                                filter = state.filter,
                                selectedIds = state.selectedIds,
                                onFilterChange = viewModel::setFilter,
                                onTogglePhoto = viewModel::togglePhoto,
                                onOpenPhotos = { photos, index -> candidateViewer = CandidateViewer(photos, index) },
                                onSelectAll = viewModel::selectCurrentFilter,
                                onInvert = viewModel::invertCurrentFilter,
                                onSkipGroup = viewModel::skipGroup,
                                onFinish = {
                                    viewModel.confirmSelection()
                                    if (state.source == ScanSource.FullAlbum) viewModel.archiveSelection()
                                    flowStep = FlowStep.Result
                                },
                            )
                            FlowStep.Result -> ResultScreen(
                                photos = state.photos,
                                selectedCount = state.selectedIds.size,
                                elapsedMillis = state.progress.elapsedMillis,
                                archive = state.archive,
                                archiving = state.archiving,
                                archiveDone = state.archiveDone,
                                archiveTotal = state.archiveTotal,
                                onViewLibrary = {
                                    flowStep = null
                                    currentTab = AppTab.Library
                                    viewModel.refreshLibrary()
                                },
                                onBackConfirm = { flowStep = FlowStep.Confirm },
                                onBackHome = {
                                    flowStep = null
                                    currentTab = AppTab.Courses
                                    flowOrigin = AppTab.Courses
                                },
                                onRescan = { startFullScan(sessionIds = lastScanSessionIds) },
                            )
                        }
                    }
                }
            }
        }
    }

    photoViewer?.let { viewer ->
        Dialog(
            onDismissRequest = { photoViewer = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            FullscreenPhotoScreen(
                photos = viewer.photos,
                startIndex = viewer.index,
                onBack = { photoViewer = null },
            )
        }
    }

    candidateViewer?.let { viewer ->
        Dialog(
            onDismissRequest = { candidateViewer = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            CandidateFullscreenScreen(
                photos = viewer.photos,
                groups = state.groups,
                startIndex = viewer.index,
                selectedIds = state.selectedIds,
                onToggle = viewModel::togglePhoto,
                onBack = { candidateViewer = null },
                onRetry = viewModel::retryPhoto,
            )
        }
    }
}

@Composable
private fun FlowHeader(step: FlowStep, onBack: () -> Unit) {
    val steps = listOf(FlowStep.Scan, FlowStep.Confirm, FlowStep.Result)
    val labels = listOf("扫描", "确认", "完成")
    val currentIndex = steps.indexOf(step)
    val skin = LocalSkin.current
    Surface(color = PaperBackground) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIconButton(icon = ClassrollIcons.ArrowLeft, contentDescription = "返回", onClick = onBack)
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    labels.forEachIndexed { index, label ->
                        if (index > 0) Box(modifier = Modifier.width(16.dp).height(1.dp).background(MaterialTheme.colorScheme.outline))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                index == currentIndex -> PaperInk
                                index < currentIndex -> PaperGood
                                else -> PaperMuted
                            },
                            modifier = Modifier.padding(horizontal = 5.dp),
                        )
                        if (index < currentIndex) Icon(ClassrollIcons.Check, contentDescription = null, tint = PaperGood, modifier = Modifier.size(12.dp))
                    }
                }
                Text("${currentIndex + 1} / 3", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 10.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(skin.cardBorder.width)
                    .background(MaterialTheme.colorScheme.outline),
            )
        }
    }
}

@Composable
private fun BottomNavigation(
    selectedTab: AppTab,
    scanning: Boolean,
    onTab: (AppTab) -> Unit,
    onScan: () -> Unit,
) {
    val skin = LocalSkin.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .skinShadow(10.dp, .10f, .22f, RoundedCornerShape(skin.radii.bar)),
        shape = RoundedCornerShape(skin.radii.bar),
        color = skin.card,
        border = if (skin.hardShadow != null) skin.cardBorder else skin.hairline,
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(62.dp)) {
            TabBarItem("课程", ClassrollIcons.Book, !scanning && selectedTab == AppTab.Courses) { onTab(AppTab.Courses) }
            TabBarItem("扫描", ClassrollIcons.Scan, scanning, onScan)
            TabBarItem("归档", ClassrollIcons.FolderCheck, !scanning && selectedTab == AppTab.Library) { onTab(AppTab.Library) }
            TabBarItem("设置", ClassrollIcons.Settings, !scanning && selectedTab == AppTab.Settings) { onTab(AppTab.Settings) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val skin = LocalSkin.current
    val bauhaus = skin.hardShadow != null
    val selectedBg = if (bauhaus) skin.accent else skin.highlight
    val tint = when {
        !selected -> skin.muted
        bauhaus -> skin.onAccent
        else -> skin.ink
    }
    val highlightShape = RoundedCornerShape(skin.corner(16.dp))
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 5.dp)
            .then(
                if (selected) {
                    Modifier
                        .background(selectedBg, highlightShape)
                        .then(if (bauhaus) Modifier.border(skin.hairline, highlightShape) else Modifier)
                } else {
                    Modifier
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(21.dp),
            tint = tint,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = tint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
