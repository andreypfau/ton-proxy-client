package utils

internal fun ByteArray.hex(separator: String = "") = joinToString(separator) {
    it.toUByte().toString(16).padStart(2, '0')
}