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
}