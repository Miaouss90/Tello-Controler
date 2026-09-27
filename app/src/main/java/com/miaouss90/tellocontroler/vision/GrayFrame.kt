package com.miaouss90.tellocontroler.vision

/** Pure 8-bit luminance image, row-major, values 0..255. */
class GrayFrame(val width: Int, val height: Int, val pixels: IntArray) {
    init {
        require(pixels.size == width * height) { "pixels size ${pixels.size} != $width x $height" }
    }

    operator fun get(x: Int, y: Int): Int = pixels[y * width + x]

    companion object {
        /** Width/height of the frames grabbed from the video for vision (1/4 of 960x720). */
        const val VISION_WIDTH = 240
        const val VISION_HEIGHT = 180

        /** ITU-R BT.601 luma from packed ARGB. */
        fun luma(argb: Int): Int {
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            return (r * 77 + g * 150 + b * 29) shr 8
        }

        fun fromArgb(width: Int, height: Int, argb: IntArray) = GrayFrame(width, height, IntArray(argb.size) { luma(argb[it]) })
    }
}
