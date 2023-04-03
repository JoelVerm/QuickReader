package com.j4a.quickreader

import android.graphics.Bitmap
import android.graphics.Color
import android.media.Image
import androidx.core.graphics.scale

fun <T>enumerate(arr: Array<T>): List<Pair<Int, T>> {
    return arr.indices.zip(arr)
}
fun enumerate(arr: IntArray): List<Pair<Int, Int>> {
    return arr.indices.zip(arr.toTypedArray())
}

class ImageBitWriter() {
    fun write(bitArray: Array<IntArray>): Bitmap {
        val bitmap = createBitmap(bitArray)
        return bitmap
    }
    private fun createBitmap(bitArray: Array<IntArray>): Bitmap {
        val QRCode = Bitmap.createBitmap(21, 21, Bitmap.Config.ARGB_8888)
        for ((y, row) in enumerate(bitArray)) {
            for ((x, value) in enumerate(row)) {
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