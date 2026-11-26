package com.example.edutrack.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.entity.SubjectEntity

/**
 * Room relation pairing a [StudySessionEntity] with its parent [SubjectEntity].
 */
data class SessionWithSubject(
    @Embedded val session: StudySessionEntity,
    @Relation(
        parentColumn = "subjectId",
        entityColumn = "id"
    )
    val subject: SubjectEntity
)
