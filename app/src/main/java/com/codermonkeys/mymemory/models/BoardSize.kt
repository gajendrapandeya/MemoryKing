package com.codermonkeys.mymemory.models



enum class BoardSize(val numCards: Int) {
    EASY(8),
    MEDIUM(18),
    HARD(24);

    fun getWidth() = when(this) {
        EASY -> 2
        MEDIUM -> 3
        HARD -> 4
    }

    companion object {
        fun getByValue(value: Int) = values().first { it.numCards == value }
    }

    fun getHeight() = numCards / getWidth()

    fun getNumPairs() = numCards / 2

    fun getNoOfMovesLeft() = when(this) {
        EASY -> 6
        MEDIUM -> 12
        HARD -> 18
    }
}