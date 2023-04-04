package com.j4a.quickreader
import android.util.Log
import kotlin.math.*
import java.nio.charset.Charset

class QREncoder {
    fun createQR(qrData: String): Array<IntArray> {
        val encodingType = checkType(qrData)
        val encodedData = encodeData(encodingType, qrData)
        val modeIndicator = getModeIndicator(encodingType)
        val charCountIndicator = getCharCountIndicator(encodingType, qrData)
        val errorCorrectionLevel = getErrorCorrectionLevel()
        val qrBits = getQrBits(errorCorrectionLevel, modeIndicator, charCountIndicator, encodedData)
        errorCorrection(qrBits, errorCorrectionLevel)
        return arrayOf(intArrayOf())
    }

    private fun errorCorrection(qrBits: String,
                                errorCorrectionLevel: Char,
    ) {
        var binaryNumbers = intArrayOf()
        for (i in 0 until (qrBits.length / 8)) {
            binaryNumbers += Integer.parseInt(qrBits.slice((i * 8) until (((i + 1) * 8))), 2)
        }
        Log.d("QR debug log", "1")
        val messagePolynomial = Array(binaryNumbers.size) {IntArray(2) {0} }
        for (i in messagePolynomial.indices) {
            messagePolynomial[i][0] = binaryNumbers[i] // coëfficient
            messagePolynomial[i][1] = messagePolynomial.size - i - 1 // power of x
        }
        Log.d("QR debug log", "2")
        var numberOfErrorCorrectionCodeWords = 0
        when(errorCorrectionLevel) {
            'L' -> numberOfErrorCorrectionCodeWords = 7
            'M' -> numberOfErrorCorrectionCodeWords = 10
            'Q' -> numberOfErrorCorrectionCodeWords = 13
            'H' -> numberOfErrorCorrectionCodeWords = 17
        }
        // sign, power of a, power of x
        val generatorPolynomial = getGeneratorPolynomial(numberOfErrorCorrectionCodeWords = numberOfErrorCorrectionCodeWords)
        Log.d("QR genPoly", generatorPolynomial.contentDeepToString())
        for (term in messagePolynomial) { // verhoogd exponent van x in messagepolynomial
            term[1] += numberOfErrorCorrectionCodeWords
        }
        val multiplyExponent = messagePolynomial[0][1] - generatorPolynomial[0][1]
        for (term in generatorPolynomial) { // maakt lead term exponent van x in generatorpolynomial gelijk aan lead term exponent in messagepolynomial
            term[1] += multiplyExponent
        }
        Log.d("QR debug log", "3")
        val errorCorrectionCodeWords = IntArray(numberOfErrorCorrectionCodeWords)
        var z = generatorPolynomial.clone()
        for (i in 1..messagePolynomial.size) { // voer stappen evenvaak uit als lengte van messagepolynomial
            val alphaMultiplyExponent: Int = if (i == 1) {
                antiLog(messagePolynomial[0][0])
            } else {
                antiLog(z[0][0])
            }
            Log.d("QR debug log", "4")
            z = generatorPolynomial.clone()
            for (j in z) { // vermenigvuldig generatorpolynomial met lead term van messagepolynomial
                j[0] = log((j[0] + alphaMultiplyExponent) % 255) // maakt cijfer coefficienten
            }
            Log.d("QR debug log", "5")
            val n = Array(messagePolynomial.size) {IntArray(2) {0}}
            for (counter in messagePolynomial.indices) {
                n[counter][1] = messagePolynomial[counter][1]
                if (counter < z.size) {
                    n[counter][0] = xorInt(z[counter][0], messagePolynomial[counter][0])
                }
                else {
                    n[counter][0] = xorInt(0, messagePolynomial[counter][0])
                }
            }
            Log.d("QR debug log", "6")
            val q = Array(n.size - 1) {IntArray(2) {0}}
            if (n[0][0] == 0) {
                for (j in q.indices) {
                    q[j][0] = n[j + 1][0]
                    q[j][1] = n[j + 1][1]
                }
            }
            else {
                throw Exception("Something got wrong with stupid calculations")
            }
            Log.d("QR debug log", "7")
            if (i == messagePolynomial.size) {
                for (j in q.indices) {
                    errorCorrectionCodeWords[j] = q[j][0]
                }
            }
            Log.d("QR codewords", errorCorrectionCodeWords.contentToString())
        }
    }

    private fun log(exponent: Int): Int { // van a^n -> cijfer
        var number = 1
        for (foo in 1..exponent) {
            number *= 2
            if (number >= 256) {
                number = xorInt(number, 285)
            }
        }
        return number
    }

    private fun antiLog(n: Int): Int { // van cijfer -> a^n
        var number = 1
        var counter = 0
        while (n != number) {
            counter += 1
            number *= 2
            if (number >= 256) {
                number = xorInt(number, 285)
            }
        }
        return counter
    }

    private fun getModeIndicator(encodingType: String): String {
        val modeIndicators = mapOf("numeric" to "0001", "alphaNumeric" to "0010", "byte" to "0100")
        val modeIndicator = modeIndicators[encodingType] ?: throw Exception("Invalid modeindicator")
        return modeIndicator
    }

    private fun getGeneratorPolynomial(previousPolynomial: Array<IntArray> = arrayOf(intArrayOf(0, 1), intArrayOf(0, 0)),
                                       previous_n: Int = 0, // n van meegegeven polynomial
                                       numberOfErrorCorrectionCodeWords: Int, // n waarvan je polynomial wilt hebben
    ): Array<IntArray> {
        // multiply each term by (x^1 - α^n)
        val n = previous_n + 1
        val newPolynomial = Array(previousPolynomial.size * 2){IntArray(2) {0}}
        val multiplyPolynomial = arrayOf(intArrayOf(0, 1), //(x^1 - α^n)
            intArrayOf(n, 0))
        Log.d("QR debug log gp", "1")
        var index = 0
        for (term in previousPolynomial.indices) { // voor elke term (bijv. -α^2x^3)
            for (t in 0..1) { // voor elke term in (x^1 - α^n)
                newPolynomial[index][0] = previousPolynomial[term][0] + multiplyPolynomial[t][0] // power of a (integer)
                newPolynomial[index][1] = previousPolynomial[term][1] + multiplyPolynomial[t][1] // power of x (integer)
                index += 1
            }
        }
        Log.d("QR debug log gp", "2")
        val defPolynomial = Array(n + 2){IntArray(2) {0}}
        for (powerOfX in 0..(n + 1)) {
            defPolynomial[powerOfX][1] = powerOfX
            var firstTerm = true
            for (polynomial in newPolynomial) {
                if (polynomial[1] == powerOfX) {
                    if (firstTerm) {
                        defPolynomial[powerOfX][0] = 2.0.pow(polynomial[0]).toInt()
                        firstTerm = false
                    }
                    else {
                        defPolynomial[powerOfX][0] = xorInt(2.0.pow(polynomial[0].toDouble()).toInt(), defPolynomial[powerOfX][0])
                    }
                }
            }
        }
        Log.d("QR debug log gp", "3")
        for (polynomial in defPolynomial) {
            polynomial[0] = antiLog(polynomial[0])
        }
        var pol = defPolynomial
        if (numberOfErrorCorrectionCodeWords <= n + 2) {
            pol = getGeneratorPolynomial(pol, n, numberOfErrorCorrectionCodeWords)
        }
        return pol
    }

    private fun xorInt(num1: Int, num2: Int): Int{
        var num1Binary = num1.toString(2)
        var num2Binary = num2.toString(2)
        num1Binary = num1Binary.padStart(max(num1Binary.length, num2Binary.length), '0')
        num2Binary = num2Binary.padStart(max(num1Binary.length, num2Binary.length), '0')
        var resultBinary = ""
        for (bit in num1Binary.indices) {
            resultBinary += (num1Binary[bit].code - '0'.code) xor (num2Binary[bit].code - '0'.code)
        }
        val resultDecimal = resultBinary.toInt(2)
        return resultDecimal
    }

    private fun getQrBits(errorCorrectionLevel: Char,
                          modeIndicator: String,
                          charCountIndicator: String,
                          encodedData: String,
    ): String {
        var bytes = 0
        when(errorCorrectionLevel) {
            'L' -> bytes = 19
            'M' -> bytes = 16
            'Q' -> bytes = 13
            'H' -> bytes = 9
        }
        val bits = bytes * 8
        var qrBits = modeIndicator + charCountIndicator + encodedData
        qrBits = if (bits - qrBits.length >= 4) {
            qrBits.padEnd(qrBits.length + 4, '0')
        } else {
            qrBits.padEnd(((4 - (bits - qrBits.length) % 4)) + qrBits.length, '0')
        }
        qrBits = qrBits.padEnd(8 - (qrBits.length % 8) + qrBits.length, '0')
        var counter = 0
        while (qrBits.length < bits) {
            qrBits += if ((counter % 2) == 0) {
                "11101100"
            } else {
                "00010001"
            }
            counter += 1
        }
        return qrBits
    }

    private fun getErrorCorrectionLevel(): Char {
        val errorCorrectionLevel = 'M'
        return errorCorrectionLevel
    }

    fun getCharCountIndicator(encodingType: String,
                              qrData: String,
    ): String {
        var indicator = ""
        when(encodingType) {
            "numeric" -> indicator = qrData.length.toString(2).padStart(10, '0')
            "alphaNumeric" -> indicator = qrData.length.toString(2).padStart(9, '0')
            "byte" -> indicator = qrData.length.toString(2).padStart(8, '0')
        }
        return indicator
    }

    private fun checkType(contents: String): String {
        val types = mutableMapOf("numeric" to true, "alphaNumeric" to true, "byte" to true)
        val numericChars = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        val alphaNumericChars = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',  'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', ' ', '$', '%', '*', '+', '-', '.', '/', ':')
        val charsetName = "ISO-8859-1"
        val charset = Charset.forName(charsetName)
        val bytes = contents.toByteArray(charset)
        val decodedText = String(bytes, charset)
        types["byte"] = decodedText == contents
        for (char in contents) {
            if (char !in numericChars) types["numeric"] = false
            if (char !in alphaNumericChars) types["alphaNumeric"] = false
        }
        var encodingType = ""
        run breaking@ {
            types.forEach { entry ->
                if (entry.value) {
                    encodingType = entry.key
                    return@breaking
                }
            }
        }
        if (encodingType == "")
            throw Exception("Text contains a non-encodable character")
        return encodingType
    }

    private fun encodeData(encodingType: String,
                           qrData: String): String {
        var bitsData = ""
        when (encodingType) {
            "numeric" -> {
                for (i in 0..(((qrData.length / 3)))) {
                    val slice = qrData.slice((i * 3)..min((((i + 1) * 3) - 1), qrData.length - 1))
                    bitsData += slice.toInt().toString(2)
                }
            }
            "alphaNumeric" -> {
                val alphaNumericChars = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',  'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', ' ', '$', '%', '*', '+', '-', '.', '/', ':')
                for (i in 0..(((qrData.length / 2)))) {
                    val slice = qrData.slice((i * 2)..min((((i + 1) * 2) - 1), qrData.length - 1))
                    bitsData += if (slice.length == 2) {
                        (((alphaNumericChars.indexOf(slice[0]) * 45) + (alphaNumericChars.indexOf(
                            slice[1]
                        ))).toString(2)).padStart(11, '0')
                    } else {
                        ((alphaNumericChars.indexOf(slice[0])).toString(2)).padStart(6, '0')
                    }
                }
            }
            "byte" -> {
                val charsetName = "ISO-8859-1"
                val charset = Charset.forName(charsetName)
                val byteArray = qrData.toByteArray(charset)
                for (byte in byteArray) {
                    bitsData += byte.toString(2)
                }
            }
        }
        return bitsData
    }
}