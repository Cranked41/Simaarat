package com.cranked.simaarat.dto

data class FaceCreateRequest(
    val name: String,             // yüz sahibinin adı
    val faceBase64: String       // resim verisi (ByteArray formatında)
)