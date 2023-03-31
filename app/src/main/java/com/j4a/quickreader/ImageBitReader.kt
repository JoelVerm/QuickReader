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
        val yTop = (imageHeight * 0.1).toInt()
        val yBottom = (imageHeight * 0.9).toInt()
        val xLeft = (imageWidth * 0.1).toInt()
        val xRight = (imageWidth * 0.9).toInt()

        var leftTop = 0
        var leftBottom = 0

        for (x in 0 until imageWidth) {
            if (imageArray[yTop][x] == 1)
                leftTop = x
            if (imageArray[yBottom][x] == 1)
                leftBottom = x
            if (leftTop != 0 && leftBottom != 0)
                break
        }

        var topLeft = 0
        var topRight = 0

        for (y in 0 until imageWidth) {
            if (imageArray[y][xLeft] == 1)
                topLeft = y
            if (imageArray[y][xRight] == 1)
                topRight = y
            if (topLeft != 0 && topRight != 0)
                break
        }

        for (y in imageArray.indices) {
            val startX = leftTop + (leftBottom - leftTop) * (y/imageHeight)
            imageArray[y] = imageArray[y].drop(startX).toIntArray()
        }
        imageArray = transpose(imageArray)
        for (x in imageArray.indices) {
            val startY = topLeft + (topRight - topLeft) * (x/imageWidth)
            imageArray[x] = imageArray[x].drop(startY).toIntArray()
        }
        imageArray = transpose(imageArray)
        //trim right
        return imageArray
    }

    private fun transpose(xs: Array<IntArray>): Array<IntArray> {
        val cols = xs[0].size
        val rows = xs.size
        return Array(cols) { j ->
            IntArray(rows) { i ->
                xs[i][j]
            }
        }
    }


    private fun imageToQr(imageArray: Array<IntArray>): Array<IntArray> {
        val imageHeight = imageArray.size
        val imageWidth = imageArray[0].size

        var scanPos = 0
        var scanStart = 0
        var lastValue = 0
        while (true) {
            scanPos++
            if (imageArray[scanPos][scanPos] == 1) {
                if (lastValue == 0)
                    scanStart = scanPos
            }
            else
                if (lastValue == 1) break
            lastValue = imageArray[scanPos][scanPos]
        }
        val pixelsPerBit = scanPos - scanStart

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