package com.j4a.quickreader

import android.graphics.BitmapFactory
import androidx.camera.core.ImageProxy

class ImageBitReader(private val image: ImageProxy) {
    fun read() {
        val buffer = image.planes[0].buffer
        val bytes = byteArrayOf()
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        for (p in image.planes) {
            print(p.buffer)
        }
    }

    fun jorian(){
        //Color determination
//QR code is 21x21
import android.media.Image
import android.media.Image.Plane
import android.graphics.Color

val seventhRow = arrayOf(1,1,1,1,1,1,1,0,1,0,1,0,1,0,1,1,1,1,1,1,1) //moet 2d array zijn
val image = Image("path/to/qr/code")
val colorValue
var imageWidth = image.getWidth()
var imageHeight = image.getHeight()
val imageArray = arrayOf<Array<Int>>()

val bitmap = BitmapFactory.decodeFile(image)

val totalRed = 0
val totalGreen = 0
val totalBlue = 0
val pixelCount = 0 //amount of pixels

//Get average pixel color
for (y in imageHeight) {
      for (x in imageWidth) {
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
val avgColor = Color.rgb(avgRed, avgGreen, avgBlue)

//Checking if color is black/white
for (y in imageHeight) {
      for (x in imageWidth) {
          val pixelColor = bitmap.getPixel(x, y)
          val redValue = Color.red(pixelColor)
          val greenValue = Color.green(pixelColor)
          val blueValue = Color.blue(pixelColor)
          
		  if (redValue > avgRed || greenValue > avgGreen || blueValue > avgBlue) {
              colorValue = 1 //black
          } else {
              colorValue = 0 //white
          }
          imageArray[y][x] = colorValue

      }
}
    }
}