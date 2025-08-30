package com.cranked.simaarat.dto


import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "faces")
data class Face(
    @Id
    @UuidGenerator
    val faceId: UUID? = null,
    @Column(length = 200)
    val name: String,
    @Lob
    @Column(name = "face_base64", columnDefinition = "TEXT")
    val faceBase64: String
)
