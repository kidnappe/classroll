package com.classroll.photo.recognition

import com.classroll.photo.model.CoarseEvidence
import com.classroll.photo.model.PhotoItem

object GroupHashAnalyzer {
    fun enrich(items: List<PhotoItem>): List<PhotoItem> = items.groupBy { it.groupId }.values.flatMap { group ->
        if (group.size < 3) return@flatMap group
        group.map { photo ->
            val distances = group.filter { it.id != photo.id }.map { PerceptualHash.distance(photo.coarse.perceptualHash, it.coarse.perceptualHash) }
            val sorted = distances.sorted()
            val median = sorted[sorted.size / 2]
            photo.copy(
                coarse = photo.coarse.copy(
                    nearDuplicateCount = distances.count { it <= 6 },
                    outlierDistance = median,
                ),
            )
        }
    }
}
