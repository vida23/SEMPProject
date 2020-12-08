package com.example.malortmobile.utilities

import com.example.malortmobile.data.ClockInformationData
import java.lang.StringBuilder

@kotlin.ExperimentalUnsignedTypes
class ByteArrayParser {
    companion object {
        private const val _TRUE_PARSED = 'T'
        private const val _STOP_BYTE:Byte = (-1).toByte()
        const val MAX_BYTES_AMOUNT = 16

        fun fromDataClassToByteArray(data: ClockInformationData) : ByteArray? {
            return null
        }
        //A,A,A,A,A,A,A,A,A,A,A,T/F,0-256,0-256,T/F,STOP_BYTE
        fun fromByteArrayToDataClass(buffer: ByteArray) : ClockInformationData? {
            if(buffer.size > MAX_BYTES_AMOUNT) {
                return null
            }

            val userIdBuilder = StringBuilder()
            for(index in 0 until 11) {
                userIdBuilder.append(buffer[index].toChar())
            }
            val clockedIn = buffer[11].toChar() == _TRUE_PARSED
            val radiation = StringBuilder()
                .append(if(buffer[12] == (-2).toByte()){ 0 } else { buffer[12].toUByte() })
                .append(".")
                .append(if(buffer[13] == (-2).toByte()){ 0 } else { buffer[13].toUByte() })
                .toString()
                .toFloat()
            val hazmatEquipped = buffer[14].toChar() == _TRUE_PARSED

            if(buffer[15] != _STOP_BYTE) {
                return null
            }

            return ClockInformationData(
                userIdBuilder.toString(),
                clockedIn,
                radiation,
                hazmatEquipped
            )
        }
    }
}