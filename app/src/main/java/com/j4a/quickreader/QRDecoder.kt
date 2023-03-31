package com.j4a.quickreader
import kotlin.math.*

class QRDecoder(private var qrData: Array<IntArray>) {
    fun readQR(): String {
        var contents = ""
        if (!applyMaskPattern()) {
            println("Error occured while trying to apply maskpattern")
        }
        val length: Int = getLength()
        val errorCorrectionLevel: String = getErrorCorrectionLevel().toString()
        val modeIndicator: String = getModeIndicator().toString()
        for (c in 1..length) {
            contents += sliceByte(c, modeIndicator)
        }
        return contents
    }
    private fun getErrorCorrectionLevel(): String? {
        val errorCorrectionLevels = mapOf("11" to "L", "10" to "M", "01" to "Q", "00" to "H")
        val errorCorrectionLevel: String? = errorCorrectionLevels[qrData[20][8].toString() + qrData[19][8].toString()]
        return errorCorrectionLevel
    }

    private fun getModeIndicator(): String? {
        val modeIndicators = mapOf("0001" to "numeric", "0010" to "alphanumeric", "0100" to "byte", "1000" to "kanji")
        val modeIndicator: String? = modeIndicators[qrData[20][20].toString() + qrData[20][19].toString() + qrData[19][20].toString() + qrData[19][19].toString()]
        return modeIndicator
    }

    private fun sliceByte(byte: Int,
                          modeIndicator: String = "byte"
    ): String {
        val byteLengths = mapOf("numeric" to 10, "alphanumeric" to 11, "byte" to 8, "kanji" to 13, "decimal" to 8)
        val byteLength: Int = byteLengths[modeIndicator]!!
        val sliceStart: Int = 4 + byte * 8
        val bitData = mutableListOf<Int>()
        for (doubleColumn in 20 downTo 2 step 2) { // gaat kolommen af
            if ((doubleColumn / 2) % 2 == 0) { // omhoog
                for (row in 20 downTo 1) { //gaat rijen in kolom af
                    for (column in doubleColumn downTo (doubleColumn - 1)) {
                        if (!(((row <= 8 && (column <= 8  || column >= 13)) || (row >= 13 && column <= 8) || (row == 6) || (column == 6)))) {
                            bitData.add(qrData[row][column])
                        }
                    }
                }
            }
            else { // naar beneden
                for (row in 1..20) { //gaat rijen in kolom af
                    for (column in doubleColumn downTo (doubleColumn - 1)) {
                        if (!(((row <= 8 && (column <= 8  || column >= 13)) || (row >= 13 && column <= 8) || (row == 6) || (column == 6)))) {
                            bitData.add(qrData[row][column])
                        }
                    }
                }
            }
        }
        var returnData = ""
        if (modeIndicator != "bits") {
            var index = 0
            var byteNumber = 0.0
            for (bit in bitData.subList(sliceStart, sliceStart + byteLength).reversed()) {
                byteNumber += bit * 2.0.pow(index.toDouble())
                index += 1
            }
            when(modeIndicator) {
                "decimal" -> {
                    returnData = byteNumber.toString()
                }
                "numeric" -> {
                    returnData = byteNumber.toString()
                }
                "alphanumeric" -> {
                    val alphaNumericChars = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',  'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', ' ', '$', '%', '*', '+', '-', '.', '/', ':')
                    val char1 = alphaNumericChars[(floor(byteNumber / 45)).toInt()]
                    val char2 = alphaNumericChars[(byteNumber - floor(byteNumber / 45)).toInt()]
                    returnData = char1.toString() + char2.toString()
                }
                "byte" -> {
                    returnData = ((byteNumber.toInt()).toChar()).toString()
                }
            }
        }
        return returnData
    }


    private fun getLength(): Int {
        val length: String = sliceByte(0, "decimal")
        return length.toDouble().toInt()
    }

    private fun applyMaskPattern(): Boolean {
        val maskNumber: String = qrData[18][8].toString() + qrData[17][8].toString() + qrData[16][8].toString()
        for (row in qrData.indices) {
            for (column in qrData.indices) {
                if (!(((row <= 8 && (column <= 8  || column >= 13)) || (row >= 13 && column <= 8) || (row == 6) || (column == 6)))) {
                    var turnBit = false
                    when (maskNumber) {
                        "111" -> if ((column) % 3 == 0) turnBit = true
                        "110" -> if ((row + column) % 3 == 0) turnBit = true
                        "101" -> if ((row + column) % 2 == 0) turnBit = true
                        "100" -> if ((row) % 2 == 0) turnBit = true
                        "011" -> if ((((row * column) % 3 + row * column) % 2) == 0) turnBit = true
                        "010" -> if ((((row * column) % 3 + row + column) % 2) == 0) turnBit = true
                        "001" -> if ((((floor(row.toDouble() / 2) + floor(column.toDouble() / 3)) % 2).toInt() == 0)) turnBit = true
                        "000" -> if ((((row * column) % 2) + (row * column) % 3) == 0) turnBit = true
                        else -> return false // invalid maskNumber
                    }
                    if (turnBit) {
                        qrData[row][column] = (qrData[row][column] * -1) + 1
                    }
                }
            }
        }
        return true
    }
}