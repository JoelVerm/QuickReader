package com.j4a.quickreader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import kotlin.math.max

infix fun ClosedRange<Double>.step(step: Double): Iterable<Double> {
    require(start.isFinite())
    require(endInclusive.isFinite())
    require(step > 0.0) { "Step must be positive, was: $step." }
    val sequence = generateSequence(start) { previous ->
        if (previous == Double.POSITIVE_INFINITY) return@generateSequence null
        val next = previous + step
        if (next > endInclusive) null else next
    }
    return sequence.asIterable()
}

fun Array<IntArray>.transpose(): Array<IntArray> {
    val cols = this[0].size
    val rows = this.size
    return Array(cols) { j ->
        IntArray(rows) { i ->
            this[i][j]
        }
    }
}

fun List<Int>.padEnd(size: Int) =
    this + List(if (this.size <= size) size - this.size else throw Exception("Pad size too small")) { 0 }

class ImageBitReader(private val image: ImageProxy) {
    private var bitmap: Bitmap = run {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
    private var imageWidth = bitmap.width
    private var imageHeight = bitmap.height

    fun read(imageToScreenCropRect: Rect): Array<IntArray> {
        crop(imageToScreenCropRect)
        var pixelArray = readColors()
        pixelArray = smartCrop(pixelArray)
        return imageToQr(pixelArray)
    }

    // dumb crop
    private fun crop(imageToScreenCropRect: Rect) {
        val screenCropWidth = imageToScreenCropRect.width()
        val screenCropHeight = imageToScreenCropRect.height()
        val screenCropOffsetX = 0.5 * (imageWidth - screenCropWidth)
        val screenCropOffsetY = 0.5 * (imageHeight - screenCropHeight)
        val finalCropSize = (0.8 * screenCropWidth).toInt()
        val squareCropOffsetX = (0.1 * screenCropWidth + screenCropOffsetX).toInt()
        val squareCropOffsetY = (0.5 * (screenCropHeight - finalCropSize) + screenCropOffsetY).toInt()
        bitmap = Bitmap.createBitmap(bitmap, squareCropOffsetX, squareCropOffsetY, finalCropSize, finalCropSize)
        imageWidth = bitmap.width
        imageHeight = bitmap.height
    }

    fun smartCrop(imageArr:Array<IntArray>):Array<IntArray> {
        var imageArray = imageArr

        val cropImage = {
            image: Array<IntArray> ->

            val topY = (image.size * 0.1).toInt()
            val bottomY = (image.size * 0.9).toInt()

            var topX = 0
            var topXSet = false
            var bottomX = 0
            var bottomXSet = false
            for (x in image.indices) {
                if (image[topY][x] == 1 && !topXSet) {
                    topX = x
                    topXSet = true
                }
                if (image[bottomY][x] == 1 && !bottomXSet) {
                    bottomX = x
                    bottomXSet = true
                }
                if (topXSet && bottomXSet)
                    break
            }

            val increaseX = (bottomX - topX) / 0.8
            val startX = topX - 0.1 * increaseX
            for (y in image.indices) {
                val x = max((startX + increaseX * (y.toFloat()/imageHeight)).toInt(), 0)
                image[y] = image[y].drop(x).padEnd(image[y].size).toIntArray()
            }
            image
        }

        imageArray = cropImage(imageArray)
        imageArray = imageArray.transpose()
        imageArray = cropImage(imageArray)
        imageArray = imageArray.transpose()
        return imageArray
    }

    private fun imageToQr(imageArray: Array<IntArray>): Array<IntArray> {
        val imageHeight = imageArray.size
        val imageWidth = imageArray[0].size
        val guessPixelsPerBit = run {
            var scanPos = 0
            var scanStart = 0
            var lastValue = 0
            while (true) {
                scanPos++
                if (imageArray[scanPos][scanPos] == 1) {
                    if (lastValue == 0)
                        scanStart = scanPos
                } else
                    if (lastValue == 1) break
                lastValue = imageArray[scanPos][scanPos]
            }
            scanPos - scanStart
        }
        val pixelsPerBitX = (1.5 .. 3.5 step 0.1).map {
            val y = (guessPixelsPerBit * it).toInt()
            var x = 0
            var xStart = 0
            var lastValue = 0
            while (true) {
                x++
                if (imageArray[y][x] == 1) {
                    if (lastValue == 0)
                        xStart = x
                } else
                    if (lastValue == 1) break
                lastValue = imageArray[y][x]
            }
            x - xStart
        }.run {
            this.sum() / this.size
        }
        val pixelsPerBitY = (1.5 .. 3.5 step 0.1).map {
            val x = (guessPixelsPerBit * it).toInt()
            var y = 0
            var yStart = 0
            var lastValue = 0
            while (true) {
                y++
                if (imageArray[y][x] == 1) {
                    if (lastValue == 0)
                        yStart = y
                } else
                    if (lastValue == 1) break
                lastValue = imageArray[y][x]
            }
            y - yStart
        }.run {
            this.sum() / this.size
        }

        val qrCodeHeight = imageHeight / pixelsPerBitY
        val qrCodeWidth = imageWidth / pixelsPerBitX
        val qrcodeArray = Array(qrCodeHeight) { IntArray(qrCodeWidth) }

        for (y in 0 until qrCodeHeight) {
            for (x in 0 until qrCodeWidth) {
                // sum all items in the current square bit
                val imageY = y * pixelsPerBitY
                val imageX = x * pixelsPerBitX
                val sum = imageArray.slice(imageY until (imageY + pixelsPerBitY))
                    .sumOf { it.slice(imageX until (imageX + pixelsPerBitX)).sum() }
                val average = sum.toFloat() / (pixelsPerBitX * pixelsPerBitY).toFloat()
                qrcodeArray[y][x] = if (average > 0.5) 1 else 0
            }
        }

        return qrcodeArray
    }

    private fun readColors(): Array<IntArray> {
        val imageArray = Array(imageHeight) { IntArray(imageWidth) }

        var totalRed = 0
        var totalGreen = 0
        var totalBlue = 0
        var pixelCount = 0

        //Get average pixel color
        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                val pixelColor = bitmap.getPixel(x, y)
                totalRed += Color.red(pixelColor)
                totalGreen += Color.green(pixelColor)
                totalBlue += Color.blue(pixelColor)

                pixelCount += 1
            }
        }
        val avgRed = totalRed / pixelCount
        val avgGreen = totalGreen / pixelCount
        val avgBlue = totalBlue / pixelCount
        val avgGray = (avgRed + avgGreen + avgBlue) / 3

        //Checking if color is black/white
        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                val pixelColor = bitmap.getPixel(x, y)
                val redValue = Color.red(pixelColor)
                val greenValue = Color.green(pixelColor)
                val blueValue = Color.blue(pixelColor)
                val grayValue = (redValue + greenValue + blueValue) / 3

                if (grayValue > avgGray) {
                    imageArray[y][x] = 0 //white
                } else {
                    imageArray[y][x] = 1 //black
                }
            }
        }

        return imageArray
    }
}