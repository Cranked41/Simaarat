package com.cranked.simaarat.dto

data class FaceCompareResponse(
    val similarity: Double,   // 0.0 - 1.0 arası benzerlik skoru
    val isMatch: Boolean      // eşik değerine göre eşleşti mi
)