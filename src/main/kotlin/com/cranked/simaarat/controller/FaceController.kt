package com.cranked.simaarat.controller

import com.cranked.simaarat.dto.Face
import com.cranked.simaarat.dto.FaceCreateRequest
import org.springframework.web.multipart.MultipartFile
import org.springframework.http.MediaType
import com.cranked.simaarat.dto.FaceCompareResponse
import com.cranked.simaarat.service.FaceService
import jakarta.persistence.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*



// --- Controller ---
@RestController
@RequestMapping("/api/face")
class FaceController(
    private val faceService: FaceService
) {
    @PostMapping("/create")
    fun create(@RequestBody req: FaceCreateRequest): ResponseEntity<*> =
        ResponseEntity.ok(faceService.createFace(req))

    @PostMapping("/compare", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun compare(
        @RequestParam("face1") face1: MultipartFile,
        @RequestParam("face2") face2: MultipartFile
    ): ResponseEntity<*> =
        ResponseEntity.ok(faceService.compareFaces(face1.bytes, face2.bytes))
}