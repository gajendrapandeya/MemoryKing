package com.codermonkeys.mymemory.models

import com.codermonkeys.mymemory.utils.*


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
        EASY -> MOVE_EASY
        MEDIUM -> MOVE_MEDIUM
        HARD -> MOVE_HARD
    }

}