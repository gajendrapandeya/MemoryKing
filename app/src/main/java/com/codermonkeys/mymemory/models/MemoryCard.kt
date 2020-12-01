package com.codermonkeys.mymemory.models

data class MemoryCard(
    val identifier: Int,
    var isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)
