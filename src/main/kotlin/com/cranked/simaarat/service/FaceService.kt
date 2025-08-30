package com.cranked.simaarat.service

import com.cranked.simaarat.dto.Face
import com.cranked.simaarat.dto.FaceCreateRequest
import com.cranked.simaarat.dto.FaceCompareResponse
import com.cranked.simaarat.dto.FaceCreateResponseModel
import com.cranked.simaarat.repository.FaceRepository
import org.springframework.stereotype.Service

@Service
class FaceService(
    private val faceRepository: FaceRepository
) {
    fun createFace(req: FaceCreateRequest): FaceCreateResponseModel {
        val response = faceRepository.save(
            Face(
                name = req.name,
                faceBase64 = req.faceBase64
            )
        )
        return FaceCreateResponseModel(faceId = response.faceId.toString())
    }

    fun compareFaces(face1: ByteArray, face2: ByteArray): FaceCompareResponse {
        require(face1.isNotEmpty() && face2.isNotEmpty()) { "Girdi byte dizileri boş olamaz" }

        // ByteArray -> List<Double> (0..255)
        val v1 = face1.map { (it.toInt() and 0xFF).toDouble() }
        val v2 = face2.map { (it.toInt() and 0xFF).toDouble() }

        // Uzunlukları eşitle: daha kısa olana göre kırp
        val len = kotlin.math.min(v1.size, v2.size)
        require(len > 0) { "Karşılaştırma için yeterli veri yok" }
        val a = v1.subList(0, len)
        val b = v2.subList(0, len)

        val cos = cosineSimilarity(a, b)
        val isMatch = cos >= 0.8

        return FaceCompareResponse(similarity = cos, isMatch = isMatch)
    }

    private fun cosineSimilarity(v1: List<Double>, v2: List<Double>): Double {
        var dot = 0.0
        var n1 = 0.0
        var n2 = 0.0
        for (i in v1.indices) {
            val a = v1[i]
            val b = v2[i]
            dot += a * b
            n1 += a * a
            n2 += b * b
        }
        val denom = (kotlin.math.sqrt(n1) * kotlin.math.sqrt(n2))
        return if (denom == 0.0) 0.0 else dot / denom
    }

    private fun euclideanDistance(v1: List<Double>, v2: List<Double>): Double {
        var sum = 0.0
        for (i in v1.indices) {
            val d = v1[i] - v2[i]
            sum += d * d
        }
        return kotlin.math.sqrt(sum)
    }
}