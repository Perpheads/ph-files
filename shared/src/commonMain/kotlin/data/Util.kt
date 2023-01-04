package com.perpheads.files.data

fun validateFilename(name: String): Boolean {
    return name.matches("^[-_.A-Za-z0-9]+\$".toRegex())
}