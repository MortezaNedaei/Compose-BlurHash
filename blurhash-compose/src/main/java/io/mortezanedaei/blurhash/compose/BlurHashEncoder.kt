package io.mortezanedaei.blurhash.compose

import android.graphics.Bitmap
import io.mortezanedaei.blurhash.compose.Base83.encode
import io.mortezanedaei.blurhash.compose.Utils.linearToSrgb
import io.mortezanedaei.blurhash.compose.Utils.signedPow
import io.mortezanedaei.blurhash.compose.Utils.srgbToLinear
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

internal object BlurHashEncoder {

    private const val COMPONENT_X = 8
    private const val COMPONENT_Y = 8

    private fun applyBasisFunction(
        pixels: IntArray,
        width: Int,
        height: Int,
        normalisation: Float,
        i: Int,
        j: Int,
        factors: Array<FloatArray>,
        index: Int
    ) {
        var r = 0f
        var g = 0f
        var b = 0f
        for (x in 0 until width)
            for (y in 0 until height) {
                val basis = (
                        normalisation * cos(PI * i * x / width) *
                                cos(PI * j * y / height)
                        ).toFloat()
                val pixel = pixels[y * width + x]
                r += basis * srgbToLinear((pixel shr 16) and 255)
                g += basis * srgbToLinear((pixel shr 8) and 255)
                b += basis * srgbToLinear(pixel and 255)
            }
        val scale = 1f / (width * height)
        factors[index][0] = r * scale
        factors[index][1] = g * scale
        factors[index][2] = b * scale
    }

    private fun encodeDC(value: FloatArray): Int {
        val r = linearToSrgb(value[0])
        val g = linearToSrgb(value[1])
        val b = linearToSrgb(value[2])
        return (r shl 16) + (g shl 8) + b
    }

    private fun encodeAC(value: FloatArray, maximumValue: Float): Int {
        val quantR = floor(
            max(
                0f,
                min(18f, floor(signedPow(value[0] / maximumValue, 0.5f) * 9 + 9.5f))
            )
        )
        val quantG = floor(
            max(
                0f,
                min(18f, floor(signedPow(value[1] / maximumValue, 0.5f) * 9 + 9.5f))
            )
        )
        val quantB = floor(
            max(
                0f,
                min(18f, floor(signedPow(value[2] / maximumValue, 0.5f) * 9 + 9.5f))
            )
        )
        return round(quantR * 19 * 19 + quantG * 19 + quantB).toInt()
    }

    /**
     * Calculates the blur hash from the given image with 4x4 components.
     *
     * @param bitmap the image
     * @return the blur hash
     */
    internal fun encode(bitmap: Bitmap): String {
        return encode(bitmap, COMPONENT_X, COMPONENT_Y)
    }

    /**
     * Calculates the blur hash from the given image.
     *
     * @param bitmap     the image
     * @param componentX number of components in the x dimension
     * @param componentY number of components in the y dimension
     * @return the blur hash
     */
    private fun encode(bitmap: Bitmap, componentX: Int, componentY: Int): String {
        // To increase the performance, we can find a optimal size for the image.
        // Then we can reduce the original bitmap to this size
        val (optimizedWidth, optimizedComponentX) = getOptimizedScale(bitmap.width, componentX)
        val (optimizedHeight, optimizedComponentY) = getOptimizedScale(bitmap.height, componentY)

        // Define the pixels array with our reduced size
        val pixels = IntArray(optimizedWidth * optimizedHeight)

        // To increase the performance, we can do reduction of dimensions.
        val reducedBitmap = getResizedBitmap(bitmap, optimizedWidth, optimizedHeight)
        // Traverse the bitmap with the given values and store the retrieved pixel values in the above array.
        reducedBitmap.getPixels(
            pixels,
            0,
            optimizedWidth,
            0,
            0,
            optimizedWidth,
            optimizedHeight
        )

        return encode(
            pixels,
            optimizedWidth,
            optimizedHeight,
            optimizedComponentX,
            optimizedComponentY
        )
    }

    /**
     * Calculates the blur hash from the given pixels.
     *
     * @param pixels     width * height pixels, encoded as RGB integers (0xAARRGGBB)
     * @param width      width of the bitmap
     * @param height     height of the bitmap
     * @param componentX number of components in the x dimension
     * @param componentY number of components in the y dimension
     * @return the blur hash
     */
    private fun encode(
        pixels: IntArray,
        width: Int,
        height: Int,
        componentX: Int,
        componentY: Int
    ): String {
        require(!(componentX < 1 || componentX > 9 || componentY < 1 || componentY > 9)) {
            "Blur hash must have between 1 and 9 components"
        }
        require(width * height == pixels.size) {
            "Width and height must match the pixels array"
        }
        val factors = Array(componentX * componentY) { FloatArray(3) }
        for (j in 0 until componentY) {
            for (i in 0 until componentX) {
                val normalisation = if (i == 0 && j == 0) 1f else 2f
                applyBasisFunction(
                    pixels, width, height, normalisation, i, j, factors,
                    j * componentX + i
                )
            }
        }
        val hash =
            CharArray(1 + 1 + 4 + 2 * (factors.size - 1)) // size flag + max AC + DC + 2 * AC components
        val sizeFlag = componentX - 1 + (componentY - 1) * 9
        encode(sizeFlag, 1, hash, 0)
        val maximumValue: Float
        if (factors.size > 1) {
            val actualMaximumValue = Utils.max(factors, 1, factors.size)
            val quantisedMaximumValue = floor(
                max(0f, min(82f, floor(actualMaximumValue * 166 - 0.5f)))
            )
            maximumValue = (quantisedMaximumValue + 1) / 166
            encode(round(quantisedMaximumValue).toInt(), 1, hash, 1)
        } else {
            maximumValue = 1f
            encode(0, 1, hash, 1)
        }
        val dc = factors[0]
        encode(encodeDC(dc), 4, hash, 2)
        for (i in 1 until factors.size) {
            encode(encodeAC(factors[i], maximumValue), 2, hash, 6 + 2 * (i - 1))
        }
        return String(hash)
    }

    /**
     * Reduce size of Bitmap to some specified pixels
     */
    private fun getResizedBitmap(bitmap: Bitmap, bitmapWidth: Int, bitmapHeight: Int): Bitmap {
        // Prevent the bitmap to be enlarged
        if (bitmapWidth >= bitmap.width || bitmapHeight >= bitmap.height) {
            return bitmap
        }
        return Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, true)
    }

    /**
     * To increase performance we should scale pixels to a smaller range
     */
    private fun getOptimizedScale(size: Int, componentSize: Int): Pair<Int, Int> {
        val optimizedSize = when (size) {
            in 0..300 -> {
                Pair(size, componentSize)
            }

            in 300..400 -> {
                Pair(size / 2, componentSize / 2)
            }

            in 400..800 -> {
                Pair(size / 3, componentSize / 2)
            }

            in 800..1000 -> {
                Pair(size / 3, componentSize / 3)
            }

            in 1000..3000 -> {
                Pair(size / 5, componentSize / 4)
            }

            else -> Pair(size / 10, 1)
        }
        return optimizedSize
    }
}
