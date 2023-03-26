package com.example.coroutine

fun logX(any: Any?) {
    println("""
========================
$any
Thread:${Thread.currentThread().name}        
    """.trimIndent())
}