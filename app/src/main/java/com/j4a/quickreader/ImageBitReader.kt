package com.j4a.quickreader

import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.camera.core.ImageProxy

class ImageBitReader(private val image: ImageProxy) {
    fun read() {
        val pixelArray = readColors()
    }

    fun readColors(): Array<Array<Int>> {
        val imageWidth = image.width
        val imageHeight = image.height
        val imageArray = arrayOf<Array<Int>>()

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        var totalRed = 0
        var totalGreen = 0
        var totalBlue = 0
        var pixelCount = 0

        //Get average pixel color
        for (y in 0..imageHeight) {
            for (x in 0..imageWidth) {
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
        print("$avgRed, $avgGreen, $avgBlue, $avgGray")

        //Checking if color is black/white
        for (y in 0..imageHeight) {
            for (x in 0..imageWidth) {
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