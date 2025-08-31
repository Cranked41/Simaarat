package com.cranked.simaarat.controller

import com.cranked.simaarat.dto.FaceCreateRequest
import org.springframework.web.multipart.MultipartFile
import org.springframework.http.MediaType
import com.cranked.simaarat.service.FaceService
import com.cranked.simaarat.utils.FaceUtil
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

    @PostMapping(
        "/compare",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun compare(
        @RequestParam("face1") face1: MultipartFile,
        @RequestParam("face2") face2: MultipartFile,
        @RequestParam("threshHold") threshHold: String,
    ): ResponseEntity<*> =
        ResponseEntity.ok(FaceUtil.compareFaces(face1.bytes, face2.bytes, threshHold))

    @GetMapping("/{id}")
    fun getFaceById(@PathVariable id: String): ResponseEntity<*> =
        ResponseEntity.ok(faceService.getFaceInformations(id))

    @GetMapping("/landmarks", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getFaceLandmarks(
        @RequestParam("face") face: MultipartFile,
    ): ResponseEntity<*> =
        ResponseEntity.ok(FaceUtil.getLandmarksWithBase64(face = face.bytes))

    @GetMapping("/landmarks-visual", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getFaceLandmarksByVisual(
        @RequestParam("face") face: MultipartFile,
    ): ResponseEntity<*> =
        ResponseEntity.ok(faceService.extractLandmarksBase64(imageBytes = face.bytes))

    @GetMapping(
        "/region-crop-mask-b64",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun cropRegionMaskedBase64(
        @RequestParam("face") face: MultipartFile,
        @RequestParam("region") region: String,
        @RequestParam("shape", required = false) shape: String?,
        @RequestParam("padding", required = false) padding: Int?
    ): ResponseEntity<*> =
        ResponseEntity.ok(faceService.cropRegionMaskedBase64(face.bytes, region, shape, padding))
}