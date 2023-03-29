package com.j4a.quickreader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import androidx.camera.core.ImageProxy

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
        val pixelArray = readColors()
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

    private fun imageToQr(imageArray: Array<IntArray>): Array<IntArray> {
        val imageHeight = imageArray.size
        val imageWidth = imageArray[0].size

        var scanPos = 0
        while (imageArray[scanPos][scanPos] == 1)
            scanPos++
        val pixelsPerBit = scanPos

        val qrCodeHeight = imageHeight / pixelsPerBit
        val qrCodeWidth = imageWidth / pixelsPerBit
        val qrcodeArray = Array(qrCodeHeight) { IntArray(qrCodeWidth) }

        for (y in 0 until qrCodeHeight) {
            for (x in 0 until qrCodeWidth) {
                // sum all items in the current square bit
                val imageY = y * pixelsPerBit
                val imageX = x * pixelsPerBit
                val sum = imageArray.slice(imageY until (imageY + pixelsPerBit))
                    .sumOf { it.slice(imageX until (imageX + pixelsPerBit)).sum() }
                val average = sum.toFloat() / (pixelsPerBit * pixelsPerBit).toFloat()
                qrcodeArray[y][x] = if (average > 0.5) 1 else 0
            }
        }

        return qrcodeArray
    }

    private fun imageToQrLegacy(imageArray: Array<IntArray>): Array<IntArray> {
        val imageHeight = imageArray.size
        val imageWidth = imageArray[0].size
        val qrcodeArray = Array(21) { IntArray(21) }

        val storeCounts = mutableListOf<Int>()
        referenceFound@ for (y in 0 until imageHeight) {
            var countBlack = 0
            var countWhite = 0
            var blackStart = imageArray[0][0]
            var whiteStart = (imageArray[0][0] * -1 + 1)
            for (x in 0 until imageWidth) {
                if (whiteStart == 1) {
                    if (imageArray[y][x] == 0) {
                        countWhite += 1
                    } else {
                        whiteStart = 0
                        blackStart = 1
                        countBlack = 1
                        storeCounts.add(countWhite)
                        continue
                    }
                }
                if (blackStart == 1) {
                    if (imageArray[y][x] == 1) {
                        countBlack += 1
                    } else {
                        whiteStart = 1
                        blackStart = 0
                        countWhite = 1
                        storeCounts.add(countBlack)
                        continue
                    }
                }
            }
            if (whiteStart == 1) {
                storeCounts.add(countWhite)
            }
            if (blackStart == 1) {
                storeCounts.add(countBlack)
            }
            if (storeCounts.size == 11) {
                if (storeCounts[1] == storeCounts[9]) {
                    var foundPattern = true
                    val lowerLimit = 6.9
                    val upperLimit = 7.1
                    for (num in 2..8) {
                        if (!(storeCounts[num].toDouble() <= storeCounts[1] / lowerLimit && storeCounts[num].toDouble() >= storeCounts[1] / upperLimit)) {
                            foundPattern = false
                        }
                    }
                    if (foundPattern) {
                        val pixelsBox = storeCounts[1] / 7.0
                        val pixelsMargin = storeCounts[0].toDouble()
                        for (y_in in 0..20) {
                            for (x_in in 0..20) {
                                qrcodeArray[y_in][x_in] =
                                    imageArray[(pixelsMargin + pixelsBox / 2 + y_in * pixelsBox).toInt()][(pixelsMargin + pixelsBox / 2 + x_in * pixelsBox).toInt()]
                            }
                        }
                        break@referenceFound
                    }
                }
            }
            storeCounts.clear()
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