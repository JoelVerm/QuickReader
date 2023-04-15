package com.j4a.quickreader
import kotlin.math.*
import java.nio.charset.Charset

class QREncoder {
    fun createQR(qrData: String): Array<IntArray> {
        val qrVersion = 1
        val encodingType = checkType(qrData)
        val encodedData = encodeData(encodingType, qrData)
        val modeIndicator = getModeIndicator(encodingType)
        val charCountIndicator = getCharCountIndicator(encodingType, qrData)
        val errorCorrectionLevel = getErrorCorrectionLevel()
        val qrBits = getQrBits(errorCorrectionLevel, modeIndicator, charCountIndicator, encodedData)
        val errorCorrectionCodeWords = getErrorCorrectionCodeWords(qrBits, errorCorrectionLevel)
        val qrMatrix = getQrMatrix(
            errorCorrectionLevel,
            qrBits,
            errorCorrectionCodeWords,
            qrVersion
        )
        return qrMatrix
    }

    private fun getQrMatrix(
        errorCorrectionLevel: Char,
        qrBits: String,
        errorCorrectionCodeWords: String,
        qrVersion: Int,
    ): Array<IntArray> {
        val qrSize = (((qrVersion - 1) * 4) + 21)
        var qrMatrix = Array(qrSize) { IntArray(qrSize) {0}}
        val finderPattern = arrayOf(intArrayOf(1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 1, 1, 1, 0, 1),
            intArrayOf(1, 0, 1, 1, 1, 0, 1),
            intArrayOf(1, 0, 1, 1, 1, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1))
        val finderPatternPositions = arrayOf(intArrayOf(0, 0), intArrayOf(0, qrSize - 7), intArrayOf(qrSize - 7, 0))
        for (finderPatternPosition in finderPatternPositions) { // finder patterns
            val finderRow = finderPatternPosition[0]
            val finderColumn = finderPatternPosition[1]
            for (row in 0 until 7) {
                for (column in 0 until 7) {
                    qrMatrix[row + finderRow][column + finderColumn] = finderPattern[row][column]
                }
            }
        }
        for (index in 8 until (qrSize - 8)) { // timing patterns
            if (index % 2 == 0) {
                qrMatrix[6][index] = 1
                qrMatrix[index][6] = 1
            }
        }
        qrMatrix[(4 * qrVersion) + 9][8] = 1 // dark module
        val errorCorrectionLevelBitsMap = mapOf('L' to "11", 'M' to "10", 'Q' to "01", 'H' to "00")
        val errorCorrectionLevelBits = errorCorrectionLevelBitsMap[errorCorrectionLevel]
        val maskNumber = getMaskPattern()
        val formatString = errorCorrectionLevelBits + maskNumber
        for (index in formatString.indices) { // zet formatstring in qrcode
            qrMatrix[20 - index][8] = formatString[index].code - '0'.code
            qrMatrix[8][index] = formatString[index].code - '0'.code
        }
        val totalQrData = qrBits + errorCorrectionCodeWords
        var index = 0
        for (doubleColumn in 20 downTo 8 step 2) { // gaat kolommen af
            if ((doubleColumn / 2) % 2 == 0) { // omhoog
                for (row in 20 downTo 1) { //gaat rijen in kolom af
                    for (column in doubleColumn downTo (doubleColumn - 1)) {
                        if (!(((row <= 8 && (column <= 8  || column >= 13)) || (row >= 13 && column <= 8) || (row == 6) || (column == 6)))) {
                            qrMatrix[row][column] = totalQrData[index].code - '0'.code
                            index += 1
                        }
                    }
                }
            }
            else { // naar beneden
                for (row in 1..20) { //gaat rijen in kolom af
                    for (column in doubleColumn downTo (doubleColumn - 1)) {
                        if (!(((row <= 8 && (column <= 8  || column >= 13)) || (row >= 13 && column <= 8) || (row == 6) || (column == 6)))) {
                            qrMatrix[row][column] = totalQrData[index].code - '0'.code
                            index += 1
                        }
                    }
                }
            }
        }
        for (doubleColumn in 5 downTo 1 step 2) {
            if (((doubleColumn - 1) / 2) % 2 == 0) { // down
                for (row in 9..12) {
                    for (column in doubleColumn downTo (doubleColumn - 1)) {
                        qrMatrix[row][column] = totalQrData[index].code - '0'.code
                        index += 1
                    }
                }
            }
            else {
                for (row in 12 downTo 9) {
                    for (column in doubleColumn downTo (doubleColumn - 1)) {
                        qrMatrix[row][column] = totalQrData[index].code - '0'.code
                        index += 1
                    }
                }
            }
        }
        qrMatrix = applyMaskPattern(qrMatrix, maskNumber, qrSize)
        return qrMatrix
    }

    private fun getMaskPattern(): String {
        return "011"
    }

    private fun applyMaskPattern(qrMatrix: Array<IntArray>,
                                 maskNumber: String,
                                 qrSize: Int,
    ): Array<IntArray> {
        for (row in 0 until qrSize) {
            for (column in 0 until qrSize) {
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
                    }
                    if (turnBit) {
                        qrMatrix[row][column] = (qrMatrix[row][column] * -1) + 1
                    }
                }
            }
        }
        return qrMatrix
    }


    private fun getErrorCorrectionCodeWords(qrBits: String,
                                            errorCorrectionLevel: Char,
    ): String {
        var binaryNumbers = intArrayOf()
        for (i in 0 until (qrBits.length / 8)) {
            binaryNumbers += Integer.parseInt(qrBits.slice((i * 8) until (((i + 1) * 8))), 2)
        }
        val messagePolynomial = Array(binaryNumbers.size) {IntArray(2) {0} }
        for (i in messagePolynomial.indices) {
            messagePolynomial[i][0] = binaryNumbers[i] // coëfficient
            messagePolynomial[i][1] = messagePolynomial.size - i - 1 // power of x
        }
        var numberOfErrorCorrectionCodeWords = 0
        when(errorCorrectionLevel) {
            'L' -> numberOfErrorCorrectionCodeWords = 7
            'M' -> numberOfErrorCorrectionCodeWords = 10
            'Q' -> numberOfErrorCorrectionCodeWords = 13
            'H' -> numberOfErrorCorrectionCodeWords = 17
        }
        // power of a, power of x
        val generatorPolynomial = getGeneratorPolynomial(numberOfErrorCorrectionCodeWords = numberOfErrorCorrectionCodeWords)
        generatorPolynomial.reverse()
        for (term in messagePolynomial) { // verhoogd exponent van x in messagepolynomial
            term[1] += numberOfErrorCorrectionCodeWords
        }
        val multiplyExponent = messagePolynomial[0][1] - generatorPolynomial[0][1]
        for (term in generatorPolynomial) { // maakt lead term exponent van x in generatorpolynomial gelijk aan lead term exponent in messagepolynomial
            term[1] += multiplyExponent
        }
        val errorCorrectionCodeWords = IntArray(numberOfErrorCorrectionCodeWords)
        var z = clone(generatorPolynomial)
        var q = Array(z.size) {IntArray(2) {0}}
        for (i in 1..messagePolynomial.size) { // voer stappen evenvaak uit als lengte van messagepolynomial
            val alphaMultiplyExponent: Int = if (i == 1) {
                antiLog(messagePolynomial[0][0])
            } else {
                antiLog(q[0][0])
            }
            z = clone(generatorPolynomial)
            for (j in z) { // vermenigvuldig generatorpolynomial met lead term van messagepolynomial
                j[0] = log((j[0] + alphaMultiplyExponent) % 255) // maakt cijfer coefficienten
            }
            if (i == 1) {
                q = messagePolynomial
            }
            val n = Array(max(q.size, z.size)) {IntArray(2) {0}}
            for (counter in 0 until max(q.size, z.size)) {
                if (counter < z.size && counter < q.size) {
                    n[counter][1] = q[counter][1]
                    n[counter][0] = xorInt(z[counter][0], q[counter][0])
                }
                else if (counter >= q.size) {
                    n[counter][1] = z[counter][1]
                    n[counter][0] = xorInt(0, z[counter][0])
                }
                else {
                    n[counter][1] = q[counter][1]
                    n[counter][0] = xorInt(0, q[counter][0])
                }
            }
            q = Array(n.size - 1) {IntArray(2) {0}}
            if (n[0][0] == 0) {
                for (j in q.indices) {
                    q[j][0] = n[j + 1][0]
                    q[j][1] = n[j + 1][1]
                }
            }
            else {
                throw Exception("Something got wrong with stupid calculations at index $i")
            }
            if (i == messagePolynomial.size) {
                for (j in q.indices) {
                    errorCorrectionCodeWords[j] = q[j][0]
                }
            }
        }
        var errorCorrectionCodeWordsBits = ""
        for (errorCorrectionCodeWord in errorCorrectionCodeWords) {
            errorCorrectionCodeWordsBits += errorCorrectionCodeWord.toString(2).padStart(8, '0')
        }
        return errorCorrectionCodeWordsBits
    }

    private fun clone(array: Array<IntArray>): Array<IntArray> {
        val clone = arrayOfNulls<IntArray>(array.size)
        for (i in array.indices) {
            clone[i] = array[i].copyOf(array[i].size)
        }
        val nonNullArray: Array<IntArray> = clone
            ?.filterNotNull()
            ?.toTypedArray() ?: emptyArray()
        return nonNullArray
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
        val n = previous_n
        val newPolynomial = Array(previousPolynomial.size * 2){IntArray(2) {0}}
        val multiplyPolynomial = arrayOf(intArrayOf(0, 1), //(x^1 - α^n)
            intArrayOf(n + 1, 0))
        var index = 0
        for (term in previousPolynomial.indices) { // voor elke term (bijv. -α^2x^3)
            for (t in 0..1) { // voor elke term in (x^1 - α^n)
                newPolynomial[index][0] = previousPolynomial[term][0] + multiplyPolynomial[t][0] // power of a (integer)
                newPolynomial[index][1] = previousPolynomial[term][1] + multiplyPolynomial[t][1] // power of x (integer)
                index += 1
            }
        }
        val defPolynomial = Array(n + 3){IntArray(2) {0}}
        for (powerOfX in 0..(n + 2)) {
            defPolynomial[powerOfX][1] = powerOfX
            var firstTerm = true
            for (polynomial in newPolynomial) {
                if (polynomial[1] == powerOfX) {
                    if (firstTerm) {
                        defPolynomial[powerOfX][0] = log(polynomial[0])
                        firstTerm = false
                    }
                    else {
                        defPolynomial[powerOfX][0] = xorInt(log(polynomial[0]), defPolynomial[powerOfX][0])
                    }
                }
            }
        }
        for (polynomial in defPolynomial) {
            polynomial[0] = antiLog(polynomial[0])
        }
        var pol = defPolynomial
        if (n + 3 <= numberOfErrorCorrectionCodeWords) {
            pol = getGeneratorPolynomial(pol, n + 1, numberOfErrorCorrectionCodeWords)
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
            resultBinary += num1Binary[bit].code xor num2Binary[bit].code
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

    private fun getCharCountIndicator(encodingType: String,
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
                    encodingType = (entry.key)
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