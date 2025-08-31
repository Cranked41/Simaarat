package com.cranked.simaarat.enums

enum class FaceLandMark(val index: Int, val region: String) {
    // Jawline (çene hattı)
    JAWLINE_0(0, "JAWLINE"),
    JAWLINE_1(1, "JAWLINE"),
    JAWLINE_2(2, "JAWLINE"),
    JAWLINE_3(3, "JAWLINE"),
    JAWLINE_4(4, "JAWLINE"),
    JAWLINE_5(5, "JAWLINE"),
    JAWLINE_6(6, "JAWLINE"),
    JAWLINE_7(7, "JAWLINE"),
    JAWLINE_8(8, "JAWLINE"),
    JAWLINE_9(9, "JAWLINE"),
    JAWLINE_10(10, "JAWLINE"),
    JAWLINE_11(11, "JAWLINE"),
    JAWLINE_12(12, "JAWLINE"),
    JAWLINE_13(13, "JAWLINE"),
    JAWLINE_14(14, "JAWLINE"),
    JAWLINE_15(15, "JAWLINE"),
    JAWLINE_16(16, "JAWLINE"),

    // Right eyebrow (sağ kaş)
    RIGHT_EYEBROW_17(17, "RIGHT_EYEBROW"),
    RIGHT_EYEBROW_18(18, "RIGHT_EYEBROW"),
    RIGHT_EYEBROW_19(19, "RIGHT_EYEBROW"),
    RIGHT_EYEBROW_20(20, "RIGHT_EYEBROW"),
    RIGHT_EYEBROW_21(21, "RIGHT_EYEBROW"),

    // Left eyebrow (sol kaş)
    LEFT_EYEBROW_22(22, "LEFT_EYEBROW"),
    LEFT_EYEBROW_23(23, "LEFT_EYEBROW"),
    LEFT_EYEBROW_24(24, "LEFT_EYEBROW"),
    LEFT_EYEBROW_25(25, "LEFT_EYEBROW"),
    LEFT_EYEBROW_26(26, "LEFT_EYEBROW"),

    // Nose bridge (burun köprüsü)
    NOSE_BRIDGE_27(27, "NOSE_BRIDGE"),
    NOSE_BRIDGE_28(28, "NOSE_BRIDGE"),
    NOSE_BRIDGE_29(29, "NOSE_BRIDGE"),
    NOSE_BRIDGE_30(30, "NOSE_BRIDGE"),

    // Nose bottom (burun ucu/kanatlar)
    NOSE_BOTTOM_31(31, "NOSE_BOTTOM"),
    NOSE_BOTTOM_32(32, "NOSE_BOTTOM"),
    NOSE_BOTTOM_33(33, "NOSE_BOTTOM"),
    NOSE_BOTTOM_34(34, "NOSE_BOTTOM"),
    NOSE_BOTTOM_35(35, "NOSE_BOTTOM"),

    // Right eye (sağ göz)
    RIGHT_EYE_36(36, "RIGHT_EYE"),
    RIGHT_EYE_37(37, "RIGHT_EYE"),
    RIGHT_EYE_38(38, "RIGHT_EYE"),
    RIGHT_EYE_39(39, "RIGHT_EYE"),
    RIGHT_EYE_40(40, "RIGHT_EYE"),
    RIGHT_EYE_41(41, "RIGHT_EYE"),

    // Left eye (sol göz)
    LEFT_EYE_42(42, "LEFT_EYE"),
    LEFT_EYE_43(43, "LEFT_EYE"),
    LEFT_EYE_44(44, "LEFT_EYE"),
    LEFT_EYE_45(45, "LEFT_EYE"),
    LEFT_EYE_46(46, "LEFT_EYE"),
    LEFT_EYE_47(47, "LEFT_EYE"),

    // Mouth outer (dış ağız)
    MOUTH_OUTER_48(48, "MOUTH_OUTER"),
    MOUTH_OUTER_49(49, "MOUTH_OUTER"),
    MOUTH_OUTER_50(50, "MOUTH_OUTER"),
    MOUTH_OUTER_51(51, "MOUTH_OUTER"),
    MOUTH_OUTER_52(52, "MOUTH_OUTER"),
    MOUTH_OUTER_53(53, "MOUTH_OUTER"),
    MOUTH_OUTER_54(54, "MOUTH_OUTER"),
    MOUTH_OUTER_55(55, "MOUTH_OUTER"),
    MOUTH_OUTER_56(56, "MOUTH_OUTER"),
    MOUTH_OUTER_57(57, "MOUTH_OUTER"),
    MOUTH_OUTER_58(58, "MOUTH_OUTER"),
    MOUTH_OUTER_59(59, "MOUTH_OUTER"),

    // Mouth inner (iç ağız)
    MOUTH_INNER_60(60, "MOUTH_INNER"),
    MOUTH_INNER_61(61, "MOUTH_INNER"),
    MOUTH_INNER_62(62, "MOUTH_INNER"),
    MOUTH_INNER_63(63, "MOUTH_INNER"),
    MOUTH_INNER_64(64, "MOUTH_INNER"),
    MOUTH_INNER_65(65, "MOUTH_INNER"),
    MOUTH_INNER_66(66, "MOUTH_INNER"),
    MOUTH_INNER_67(67, "MOUTH_INNER");

    companion object {
        private val mapByIdx = entries.associateBy { it.index }
        fun fromIndex(idx: Int): FaceLandMark =
            mapByIdx[idx] ?: throw IllegalArgumentException("Invalid landmark index: $idx")
    }
}