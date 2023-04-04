package com.j4a.quickreader

import android.graphics.Bitmap
import android.graphics.Color
import android.media.Image
import androidx.core.graphics.scale

fun <T>Array<T>.enumerate(): List<Pair<Int, T>> {
    return this.indices.zip(this)
}
fun IntArray.enumerate(): List<Pair<Int, Int>> {
    return this.indices.zip(this.toTypedArray())
}

class ImageBitWriter() {
    fun write(bitArray: Array<IntArray>): Bitmap {
        val bitmap = createBitmap(bitArray)
        return bitmap
    }
    private fun createBitmap(bitArray: Array<IntArray>): Bitmap {
        val QRCode = Bitmap.createBitmap(bitArray[0].size, bitArray.size, Bitmap.Config.ARGB_8888)
        for ((y, row) in bitArray.enumerate()) {
            for ((x, value) in row.enumerate()) {
                if (value == 1){
                    QRCode.setPixel(x, y,  Color.argb(255, 0, 0, 0))
                }
                else{
                    QRCode.setPixel(x, y,  Color.argb(255, 255, 255, 255))
                }
            }
        }
        return QRCode
    }
}