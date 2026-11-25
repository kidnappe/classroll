package com.classroll.photo.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object ClassrollIcons {
    val Book = strokeIcon("Book", "M4.5 6.2A2.7 2.7 0 0 1 7.2 3.5H19.5v17H7.2a2.7 2.7 0 0 1-2.7-2.7V6.2Z", "M4.5 17.6a2.7 2.7 0 0 1 2.7-2.7h12.3", "M8.5 7.2h6.8M8.5 10.4h4.6")
    val Scan = strokeIcon("Scan", "M4 8.2V5.7A1.7 1.7 0 0 1 5.7 4h2.5M15.8 4h2.5A1.7 1.7 0 0 1 20 5.7v2.5M20 15.8v2.5a1.7 1.7 0 0 1-1.7 1.7h-2.5M8.2 20H5.7A1.7 1.7 0 0 1 4 18.3v-2.5", "M3.5 12h17")
    val FolderCheck = strokeIcon("FolderCheck", "M3.5 7.2A2.2 2.2 0 0 1 5.7 5h4l2 2.4h6.6a2.2 2.2 0 0 1 2.2 2.2v8.2a2.2 2.2 0 0 1-2.2 2.2H5.7a2.2 2.2 0 0 1-2.2-2.2V7.2Z", "m8.8 13.6 2.2 2.2 4.4-4.6")
    val Settings = strokeIcon("Settings", "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09A1.65 1.65 0 0 0 15 4.6a1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z", "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z")
    val ArrowLeft = strokeIcon("ArrowLeft", "M20 12H5M10 6.8 4.8 12l5.2 5.2")
    val ArrowRight = strokeIcon("ArrowRight", "M4 12h15M14 6.8l5.2 5.2-5.2 5.2")
    val ChevronRight = strokeIcon("ChevronRight", "m9 5.5 6.5 6.5L9 18.5")
    val ChevronLeft = strokeIcon("ChevronLeft", "m15 5.5-6.5 6.5L15 18.5")
    val ChevronDown = strokeIcon("ChevronDown", "m5.5 9 6.5 6.5L18.5 9")
    val Close = strokeIcon("Close", "m6 6 12 12M18 6 6 18")
    val Check = strokeIcon("Check", "m4.8 12.6 4.6 4.6L19.4 7")
    val Upload = strokeIcon("Upload", "M4.5 15.5v3.3a1.7 1.7 0 0 0 1.7 1.7h11.6a1.7 1.7 0 0 0 1.7-1.7v-3.3", "M12 16V3.8M7.6 8.2 12 3.8l4.4 4.4")
    val Shield = strokeIcon("Shield", "M12 3.2 20 6v6.1c0 5-3.3 7.9-8 9.7-4.7-1.8-8-4.7-8-9.7V6l8-2.8Z", "m8.7 12.2 2.2 2.2 4.4-4.5")
    val Image = strokeIcon("Image", "M3.5 4.5h17v15h-17z", "M8.7 9.5a1.6 1.6 0 1 1-3.2 0 1.6 1.6 0 0 1 3.2 0Z", "m4.8 17.2 4.6-4.3 3.2 2.8 2.7-2.6 3.9 3.7")
    val File = strokeIcon("File", "M6 3.8h7.2l4.8 4.7v11.7H6z", "M13.2 3.8v4.8H18M9 12h6M9 15.4h6M9 8.6h1.8")
    val Refresh = strokeIcon("Refresh", "M20 5v6h-6M4 19v-6h6", "M18.5 11A7 7 0 0 0 6.4 6.7M5.5 13A7 7 0 0 0 17.6 17.3")
    val Trash = strokeIcon("Trash", "M4.5 7h15M9 7V4.8h6V7M6.8 7l.8 12.2h8.8L17.2 7M10.2 10.5v5.6M13.8 10.5v5.6")
    val Clock = strokeIcon("Clock", "M12 3.5a8.5 8.5 0 1 1-8.5 8.5 8.5 8.5 0 0 1 8.5-8.5Z", "M12 7.5V12l3.1 1.9")
    val Pin = strokeIcon("Pin", "M12 21c4-4.1 6.2-7.2 6.2-10.2A6.2 6.2 0 0 0 5.8 10.8C5.8 13.8 8 16.9 12 21Z", "M14.8 10.8a2.8 2.8 0 1 1-5.6 0 2.8 2.8 0 0 1 5.6 0Z")
    val Layers = strokeIcon("Layers", "m12 3.5 8.5 4.4L12 12.3 3.5 7.9z", "m4.5 12 7.5 3.9 7.5-3.9M4.5 16.1l7.5 3.9 7.5-3.9")
    val Alert = strokeIcon("Alert", "M12 3.8 21 20H3L12 3.8Z", "M12 9v5M12 17.2v.1")
    val Eye = strokeIcon("Eye", "M2.8 12S6.3 5.8 12 5.8 21.2 12 21.2 12 17.7 18.2 12 18.2 2.8 12 2.8 12Z", "M14.7 12a2.7 2.7 0 1 1-5.4 0 2.7 2.7 0 0 1 5.4 0Z")
    val Plus = strokeIcon("Plus", "M12 5v14M5 12h14")
    val Code = strokeIcon("Code", "m9 8-4 4 4 4", "M15 8l4 4-4 4")
    val Calendar = strokeIcon("Calendar", "M4.5 6.5h15v13h-15z", "M4.5 10.5h15", "M8.2 4v4M15.8 4v4")

    private fun strokeIcon(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        paths.forEach { path ->
            builder.addPath(
                pathData = PathParser().parsePathString(path).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
