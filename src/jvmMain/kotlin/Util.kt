package com.perpheads.files

import java.util.*


fun Random.alphaNumeric(len: Int): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..len).map {
        chars[nextInt(chars.length)]
    }.joinToString()
}