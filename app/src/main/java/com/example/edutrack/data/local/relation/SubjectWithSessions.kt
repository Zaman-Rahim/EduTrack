package com.example.edutrack.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.edutrack.data.local.entity.StudySessionEntity
import com.example.edutrack.data.local.entity.SubjectEntity

/**
 * Room relation pairing a [SubjectEntity] with all of its logged [StudySessionEntity] records.
 */
data class SubjectWithSessions(
    @Embedded val subject: SubjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "subjectId"
    )
    val sessions: List<StudySessionEntity>
)
