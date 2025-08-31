package com.cranked.simaarat.utils

import com.cranked.simaarat.dto.Face
import com.cranked.simaarat.dto.FaceCompareResponse
import jdk.jfr.Threshold
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Point2f
import org.bytedeco.opencv.opencv_core.RectVector
import org.bytedeco.opencv.opencv_core.Point2fVectorVector
import org.bytedeco.opencv.opencv_face.FacemarkKazemi
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import org.bytedeco.opencv.global.opencv_core.CV_8U
import org.bytedeco.opencv.opencv_core.Point
import org.bytedeco.opencv.opencv_core.Rect
import org.bytedeco.opencv.opencv_core.Scalar
import org.bytedeco.opencv.opencv_core.Size
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.opencv.global.opencv_core
import org.bytedeco.opencv.global.opencv_core.CV_8U
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.opencv_core.*
import org.bytedeco.opencv.opencv_face.Facemark
import org.bytedeco.opencv.opencv_face.FacemarkLBF
import kotlin.math.abs

object FaceUtil {
    private lateinit var faceCascade: CascadeClassifier
    private lateinit var facemark: Facemark
    private var engineInUse: Engine? = null

    private fun ensureModelsLoaded() {
        if (!::faceCascade.isInitialized) {
            faceCascade = CascadeClassifier(resolveCascadePath())
        }
        if (!::facemark.isInitialized) {
            facemark = FacemarkKazemi.create()
            facemark.loadModel(resolveKazemiModelPath())
        }
    }

    fun compareFaces(face1: ByteArray, face2: ByteArray, threshold: String): FaceCompareResponse {
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
        val isMatch = cos >= threshold.toInt()

        return FaceCompareResponse(similarity = cos, isMatch = isMatch)
    }

    fun getLandMarks(face: ByteArray): List<Map<String, Any>> {
        val pts = detectLandmarks(face)
        return pts.mapIndexed { idx, p ->
            mapOf(
                "index" to idx,
                "x" to p.x(),
                "y" to p.y()
            )
        }
    }

    private fun detectLandmarksWithMat(imageBytes: ByteArray): Pair<List<Point2f>, Mat> {
        ensureModelsLoaded()

        val data = BytePointer(*imageBytes)
        val buf = Mat(1, imageBytes.size, CV_8U, data)
        val color: Mat = opencv_imgcodecs.imdecode(buf, opencv_imgcodecs.IMREAD_COLOR)
        if (color.empty()) {
            data.deallocate()
            return Pair(emptyList(), color)
        }

        val gray = Mat()
        opencv_imgproc.cvtColor(color, gray, opencv_imgproc.COLOR_BGR2GRAY)
        opencv_imgproc.equalizeHist(gray, gray)

        val faces = RectVector()
        faceCascade.detectMultiScale(gray, faces)
        if (faces.size() == 0L) {
            gray.release()
            data.deallocate()
            return Pair(emptyList(), color)
        }

        val landmarks = Point2fVectorVector()
        val ok = facemark.fit(color, faces, landmarks)
        if (!ok || landmarks.size() == 0L) {
            gray.release()
            data.deallocate()
            return Pair(emptyList(), color)
        }

        // En büyük yüzü seç
        var maxIdx = 0
        var maxArea = -1L
        for (i in 0 until faces.size().toInt()) {
            val r = faces.get(i.toLong())
            val area = r.width().toLong() * r.height().toLong()
            if (area > maxArea) {
                maxArea = area; maxIdx = i
            }
        }

        val ptsVec = landmarks.get(maxIdx.toLong())
        val count = ptsVec.size().toInt()
        val out = ArrayList<Point2f>(count)
        for (i in 0 until count) {
            out.add(ptsVec.get(i.toLong()))
        }

        gray.release()
        data.deallocate()
        // DİKKAT: color Mat’ini release ETME, çünkü crop için lazım
        return Pair(out, color)
    }

    fun getLandmarksWithBase64(face: ByteArray): List<LandmarkBase64Response> {
        ensureModelsLoaded()
        val (points, color) = detectLandmarksWithMat(face) // detectLandmarks'tan hem pts hem color Mat dön
        val result = mutableListOf<LandmarkBase64Response>()

        val cropSize = 30
        val half = cropSize / 2

        for ((idx, p) in points.withIndex()) {
            val cx = p.x().toInt()
            val cy = p.y().toInt()

            val x0 = (cx - half).coerceAtLeast(0)
            val y0 = (cy - half).coerceAtLeast(0)
            val x1 = (cx + half).coerceAtMost(color.cols())
            val y1 = (cy + half).coerceAtMost(color.rows())
            val w = (x1 - x0).coerceAtLeast(1)
            val h = (y1 - y0).coerceAtLeast(1)

            val roi = org.bytedeco.opencv.opencv_core.Rect(x0, y0, w, h)
            val crop = Mat(color, roi).clone()

            val buf = org.bytedeco.javacpp.BytePointer()
            opencv_imgcodecs.imencode(".png", crop, buf)
            val bytes = ByteArray(buf.limit().toInt())
            buf.get(bytes)
            val b64 = Base64.getEncoder().encodeToString(bytes)

            result.add(LandmarkBase64Response(idx, b64))

            buf.deallocate()
            crop.release()
        }
        color.release()
        return result
    }

    data class LandmarkBase64Response(
        val index: Int,
        val base64: String
    )

    private fun detectLandmarks(imageBytes: ByteArray): List<Point2f> {
        ensureModelsLoaded()

        // Decode bytes to Mat (color)
        val data = BytePointer(*imageBytes)
        val buf = Mat(1, imageBytes.size, CV_8U, data)
        val color: Mat = opencv_imgcodecs.imdecode(buf, opencv_imgcodecs.IMREAD_COLOR)
        if (color.empty()) {
            data.deallocate()
            return emptyList()
        }

        // Preprocess: gray + equalize
        val gray = Mat()
        opencv_imgproc.cvtColor(color, gray, opencv_imgproc.COLOR_BGR2GRAY)
        opencv_imgproc.equalizeHist(gray, gray)

        // Detect faces
        val faces = RectVector()
        faceCascade.detectMultiScale(gray, faces)
        if (faces.size() == 0L) {
            gray.release()
            color.release()
            data.deallocate()
            return emptyList()
        }

        // Fit landmarks
        val landmarks = Point2fVectorVector()
        val ok = facemark.fit(color, faces, landmarks)
        if (!ok || landmarks.size() == 0L) {
            gray.release()
            color.release()
            data.deallocate()
            return emptyList()
        }

        // Take landmarks of the largest face (index 0..n-1)
        var maxIdx = 0
        var maxArea = -1L
        for (i in 0 until faces.size().toInt()) {
            val r = faces.get(i.toLong())
            val area = r.width().toLong() * r.height().toLong()
            if (area > maxArea) {
                maxArea = area; maxIdx = i.toInt()
            }
        }

        val ptsVec = landmarks.get(maxIdx.toLong())
        val count = ptsVec.size().toInt()
        val out = ArrayList<Point2f>(count)
        for (i in 0 until count) {
            out.add(ptsVec.get(i.toLong()))
        }

        gray.release(); color.release()
        data.deallocate()
        return out
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


    private fun resolveCascadePath(): String =
        System.getenv("OPENCV_FACE_CASCADE")
            ?: resourceToTempFile("models/haarcascade_frontalface_alt2.xml", ".xml")

    private fun resolveKazemiModelPath(): String =
        System.getenv("OPENCV_KAZEMI_MODEL")
            ?: resourceToTempFile("models/face_landmark_model.dat", ".dat")

    // --- landmark + color overlay + legend => Base64 PNG ---
    // -------- Settings / Paths --------
    private fun resourceToTempFile(classpath: String, suffix: String): String {
        val ins: InputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(classpath)
            ?: throw IllegalStateException("Resource not found on classpath: $classpath")
        val tmp = Files.createTempFile("model_", suffix).toFile()
        tmp.deleteOnExit()
        ins.use { Files.copy(it, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        return tmp.absolutePath
    }

    private fun resourceExists(classpath: String): Boolean =
        Thread.currentThread().contextClassLoader.getResource(classpath) != null

    private fun cascadePath(): String =
        System.getenv("OPENCV_FACE_CASCADE")
            ?: resourceToTempFile("models/haarcascade_frontalface_alt2.xml", ".xml")

    private fun kazemiPath(): String =
        System.getenv("OPENCV_KAZEMI_MODEL")
            ?: resourceToTempFile("models/face_landmark_model.dat", ".dat")

    private fun lbfPath(): String =
        System.getenv("OPENCV_LBF_MODEL")
            ?: resourceToTempFile("models/lbfmodel.yaml", ".yaml")

    // -------- Engines --------
    private enum class Engine { LBF, KAZEMI }


    private fun ensureCascade() {
        if (!::faceCascade.isInitialized) faceCascade = CascadeClassifier(cascadePath())
    }

    private fun makeFacemark(engine: Engine): Facemark {
        return when (engine) {
            Engine.LBF -> {
                val fm = FacemarkLBF.create()
                fm.loadModel(lbfPath())
                fm
            }
            Engine.KAZEMI -> {
                val fm = FacemarkKazemi.create()
                fm.loadModel(kazemiPath())
                fm
            }
        }
    }

    private fun pickDefaultEngine(): Engine {
        // 1) Respect env
        when (System.getenv("FACEMARK_ENGINE")?.uppercase()) {
            "LBF" -> return Engine.LBF
            "KAZEMI" -> return Engine.KAZEMI
        }
        // 2) Prefer LBF if model exists, else Kazemi if exists, else fallback LBF (and fail later if missing)
        return when {
            resourceExists("models/lbfmodel.yaml") || System.getenv("OPENCV_LBF_MODEL") != null -> Engine.LBF
            resourceExists("models/face_landmark_model.dat") || System.getenv("OPENCV_KAZEMI_MODEL") != null -> Engine.KAZEMI
            else -> Engine.LBF
        }
    }

    private fun ensureFacemark(engine: Engine) {
        if (engineInUse != engine || facemark == null) {
            facemark = makeFacemark(engine)
            engineInUse = engine
        }
    }

    // -------- Public API --------
    /**
     * Yüz landmark bölgelerini farklı renklerle çizer, sağa legend ekler,
     * PNG olarak encode edip Base64 string döner. LBF/Kazemi arasında otomatik fallback yapar.
     */
    fun drawLandmarkRegionsWithLegend(imageBytes: ByteArray): String {
        ensureCascade()

        val data = BytePointer(*imageBytes)
        val buf = Mat(1, imageBytes.size, CV_8U, data)
        val color: Mat = opencv_imgcodecs.imdecode(buf, opencv_imgcodecs.IMREAD_COLOR)
        require(!color.empty()) { "Görsel decode edilemedi" }

        // Preprocess
        val gray = Mat()
        opencv_imgproc.cvtColor(color, gray, opencv_imgproc.COLOR_BGR2GRAY)
        opencv_imgproc.equalizeHist(gray, gray)

        // Detect faces (stabil parametreler)
        val faces = RectVector()
        faceCascade.detectMultiScale(gray, faces, 1.1, 3, 0, Size(80, 80), Size())
        if (faces.size() == 0L) {
            val outB64 = encodePngToBase64(color)
            releaseAll(data = data, mats = arrayOf(gray, color))
            return outB64
        }

        // En büyük yüzü seç
        var best = 0
        var bestArea = -1L
        for (i in 0 until faces.size().toInt()) {
            val r = faces.get(i.toLong())
            val area = r.width().toLong() * r.height().toLong()
            if (area > bestArea) { bestArea = area; best = i }
        }

        // 1) Default engine
        var engine = pickDefaultEngine()
        var pts = fitLandmarks(engine, gray, faces)
        // sanity check
        if (!isPlausible(pts)) {
            // 2) Fallback: other engine
            engine = if (engine == Engine.LBF) Engine.KAZEMI else Engine.LBF
            pts = fitLandmarks(engine, gray, faces)
        }
        // sadece seçilen yüzün noktaları
        val ptsVec = pts.get(best.toLong())
        val count = ptsVec.size().toInt()
        require(count >= 68) { "Beklenen 68 nokta bulunamadı ($count)" }

        // Çizim
        drawOverlay(color, ptsVec)

        // Legend + encode
        val out = withLegend(color)
        val outB64 = encodePngToBase64(out)

        releaseAll(
            data = data,
            mats = arrayOf(gray, color, out)
        )

        return outB64
    }

    // -------- Internals --------
    private fun fitLandmarks(engine: Engine, imgGray: Mat, faces: RectVector): Point2fVectorVector {
        ensureFacemark(engine)
        val landmarks = Point2fVectorVector()
        val ok = facemark!!.fit(imgGray, faces, landmarks) // gri üzerinde fit
        require(ok && landmarks.size() > 0) { "Landmark çıkarımı başarısız (${engine.name})" }
        return landmarks
    }

    /** Basit tutarlılık testi: gözler ağızdan yukarıda, burun köprüsü göz-altına yakın, çene ağızdan aşağıda olmalı. */
    private fun isPlausible(landmarks: Point2fVectorVector): Boolean {
        if (landmarks.size() == 0L) return false
        val pts = landmarks.get(0)  // ilk yüz
        val need = intArrayOf(36,39,42,45,30,33,8,51,57) // sağ/sol göz köşeleri, burun uç/orta, çene, ağız üst/alt
        for (i in need) if (i >= pts.size()) return false

        fun pt(i: Int) = pts.get(i.toLong())
        val eyeY = (pt(36).y() + pt(39).y() + pt(42).y() + pt(45).y()) / 4.0
        val mouthTopY = pt(51).y()
        val mouthBottomY = pt(57).y()
        val chinY = pt(8).y()
        val noseY = (pt(30).y() + pt(33).y()) / 2.0

        // Koşullar (toleranslı):
        return (eyeY + 5 < noseY) &&
                (noseY + 5 < mouthTopY) &&
                (mouthTopY + 5 < mouthBottomY) &&
                (mouthBottomY + 10 < chinY) &&
                abs(pt(36).y() - pt(45).y()) < 40 // gözler çok eğik değil
    }

    private fun drawOverlay(color: Mat, ptsVec: org.bytedeco.opencv.opencv_core.Point2fVector) {
        data class Col(val b:Int, val g:Int, val r:Int)
        val palette = mapOf(
            "LEFT_EYE"      to Col(0, 0, 255),
            "RIGHT_EYE"     to Col(255, 0, 0),
            "LEFT_EYEBROW"  to Col(0, 255, 255),
            "RIGHT_EYEBROW" to Col(255, 165, 0),
            "NOSE_BRIDGE"   to Col(255, 0, 255),
            "NOSE_BOTTOM"   to Col(0, 215, 255),
            "MOUTH_OUTER"   to Col(128, 0, 128),
            "MOUTH_INNER"   to Col(203, 192, 255),
            "JAWLINE"       to Col(34, 139, 34)
        )
        fun scalar(c: Col) = Scalar(c.b.toDouble(), c.g.toDouble(), c.r.toDouble(), 255.0)
        fun p(i: Int): Point2f = ptsVec.get(i.toLong())

        val MOUTH_OUTER = listOf(48,49,50,51,52,53,54,55,56,57,58,59)
        val MOUTH_INNER = listOf(60,61,62,63,64,65,66,67)

        val groups = listOf(
            "JAWLINE"        to (0..16).toList(),
            "RIGHT_EYEBROW"  to (17..21).toList(),
            "LEFT_EYEBROW"   to (22..26).toList(),
            "NOSE_BRIDGE"    to (27..30).toList(),
            "NOSE_BOTTOM"    to (31..35).toList(),
            "RIGHT_EYE"      to (36..41).toList(),
            "LEFT_EYE"       to (42..47).toList(),
            "MOUTH_OUTER"    to MOUTH_OUTER,
            "MOUTH_INNER"    to MOUTH_INNER
        )
        val thickness = 2
        val radius = 1

        fun drawChain(idxs: List<Int>, sc: Scalar, closed: Boolean) {
            if (idxs.size < 2) return
            for (k in idxs.indices) {
                val j = if (k == idxs.lastIndex) 0 else k + 1
                if (!closed && j == 0) break
                val a = p(idxs[k]); val b = p(idxs[j])
                opencv_imgproc.line(
                    color,
                    Point(a.x().toInt(), a.y().toInt()),
                    Point(b.x().toInt(), b.y().toInt()),
                    sc, thickness, opencv_imgproc.LINE_AA, 0
                )
            }
        }
        fun drawPoints(idxs: List<Int>, sc: Scalar) {
            for (idx in idxs) {
                val cpt = p(idx)
                opencv_imgproc.circle(
                    color, Point(cpt.x().toInt(), cpt.y().toInt()),
                    radius, sc, opencv_imgproc.FILLED, opencv_imgproc.LINE_AA, 0
                )
            }
        }
        for ((region, idxs) in groups) {
            val sc = scalar(palette[region] ?: Col(255,255,255))
            when (region) {
                "RIGHT_EYE", "LEFT_EYE", "MOUTH_OUTER", "MOUTH_INNER" ->
                    drawChain(idxs, sc, closed = true)
                else ->
                    drawChain(idxs, sc, closed = false)
            }
            drawPoints(idxs, sc)
        }
    }

    private fun withLegend(colorDrawn: Mat): Mat {
        val legendWidth = 220
        val outW = colorDrawn.cols() + legendWidth
        val outH = colorDrawn.rows()
        val out = Mat(outH, outW, colorDrawn.type(), Scalar(0.0, 0.0, 0.0, 255.0))

        val roi = out.apply(Rect(0, 0, colorDrawn.cols(), colorDrawn.rows()))
        colorDrawn.copyTo(roi)

        data class Col(val b:Int, val g:Int, val r:Int)
        fun scalar(c: Col) = Scalar(c.b.toDouble(), c.g.toDouble(), c.r.toDouble(), 255.0)
        val palette = mapOf(
            "LEFT_EYE" to Col(0,0,255), "RIGHT_EYE" to Col(255,0,0),
            "LEFT_EYEBROW" to Col(0,255,255), "RIGHT_EYEBROW" to Col(255,165,0),
            "NOSE_BRIDGE" to Col(255,0,255), "NOSE_BOTTOM" to Col(0,215,255),
            "MOUTH_OUTER" to Col(128,0,128), "MOUTH_INNER" to Col(203,192,255),
            "JAWLINE" to Col(34,139,34)
        )
        val ordered = listOf(
            "LEFT_EYE","RIGHT_EYE","LEFT_EYEBROW","RIGHT_EYEBROW",
            "NOSE_BRIDGE","NOSE_BOTTOM","MOUTH_OUTER","MOUTH_INNER","JAWLINE"
        )

        val startX = colorDrawn.cols() + 12
        var y = 20
        val box = Size(24, 14)
        val gap = 8
        val textOffset = 30
        val fontFace = opencv_imgproc.FONT_HERSHEY_SIMPLEX
        val fontScale = 0.5
        val fontThick = 1

        for (label in ordered) {
            val sc = scalar(palette[label] ?: Col(255,255,255))
            val r = Rect(startX, y, box.width(), box.height())
            opencv_imgproc.rectangle(out, r, sc, opencv_imgproc.FILLED, opencv_imgproc.LINE_8, 0)
            opencv_imgproc.putText(
                out, label,
                Point(startX + textOffset, y + box.height() - 2),
                fontFace, fontScale, Scalar(255.0,255.0,255.0,255.0),
                fontThick, opencv_imgproc.LINE_AA, false
            )
            y += box.height() + gap
        }
        return out
    }

    private fun encodePngToBase64(mat: Mat): String {
        val bp = BytePointer()
        opencv_imgcodecs.imencode(".png", mat, bp)
        val bytes = ByteArray(bp.limit().toInt()); bp.get(bytes)
        bp.deallocate()
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun releaseAll(data: BytePointer? = null, mats: Array<Mat> = emptyArray()) {
        mats.forEach { it.release() }
        data?.deallocate()
    }
}