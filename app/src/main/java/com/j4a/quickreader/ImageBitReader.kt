package com.j4a.quickreader

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import androidx.camera.core.ImageProxy

class ImageBitReader(private val image: ImageProxy) {
    fun read(): Array<IntArray> {
        val pixelArray = readColors()
        return imageToQr(pixelArray)
    }

    private fun imageToQr(imageArray: Array<IntArray>): Array<IntArray> {
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
        val imageWidth = image.width
        val imageHeight = image.height
        val imageArray = Array(imageHeight) { IntArray(imageWidth) }

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

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
        Log.d("Averages", "$avgRed, $avgGreen, $avgBlue, $avgGray")

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