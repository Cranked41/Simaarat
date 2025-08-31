package com.cranked.simaarat.service

import com.cranked.simaarat.dto.Face
import com.cranked.simaarat.dto.FaceCreateRequest
import com.cranked.simaarat.dto.FaceCreateResponseModel
import com.cranked.simaarat.repository.FaceRepository
import com.cranked.simaarat.utils.FaceUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


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

    @Transactional(readOnly = true)
    fun getFaceInformations(faceId: String) = faceRepository.findByFaceId(faceId)

    fun extractLandmarksBase64(imageBytes: ByteArray): String {

        return FaceUtil.drawLandmarkRegionsWithLegend(imageBytes)
    }

    private fun getLandmarkEnum(idx: Int): String {
        return when (idx) {
            in 0..16 -> "JAWLINE"
            in 17..21 -> "RIGHT_EYEBROW"
            in 22..26 -> "LEFT_EYEBROW"
            in 27..30 -> "NOSE_BRIDGE"
            in 31..35 -> "NOSE_BOTTOM"
            in 36..41 -> "RIGHT_EYE"
            in 42..47 -> "LEFT_EYE"
            in 48..59 -> "MOUTH_OUTLINE"
            in 60..67 -> "MOUTH_INNER"
            else -> "UNKNOWN"
        }
    }

    fun cropRegionBase64(imageBytes: ByteArray, regionName: String, padding: Int?): String =
            FaceUtil.cropRegionBase64(imageBytes, regionName, padding ?: 12)

    fun cropRegionMaskedBase64(imageBytes: ByteArray, regionName: String, shape: String?, padding: Int?): String =
            FaceUtil.cropRegionMaskedBase64(imageBytes, regionName, shape ?: "polygon", padding ?: 12)
}