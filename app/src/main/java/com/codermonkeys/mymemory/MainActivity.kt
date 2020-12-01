package com.codermonkeys.mymemory

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codermonkeys.mymemory.models.BoardSize
import com.codermonkeys.mymemory.models.MemoryCard
import com.codermonkeys.mymemory.models.MemoryGame
import com.codermonkeys.mymemory.utils.DEFAULT_ICONS
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {


    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var clRoot: ConstraintLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private lateinit var adapter: MemoryBoardAdapter

    private lateinit var memoryGame: MemoryGame
    private var boardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)

        memoryGame = MemoryGame(boardSize)

        adapter = MemoryBoardAdapter(
            this,
            boardSize,
            memoryGame.cards,
            object : MemoryBoardAdapter.CardClickListener {
                override fun onCardClicked(position: Int) {
                    updateGameWithFlip(position)
                }

            })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    private fun updateGameWithFlip(position: Int) {
        //Error Checking
        if (memoryGame.haveWonGame()) {
            //Alert the user of an invalid move
            Snackbar.make(clRoot, "You have already won the game", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (memoryGame.isCardFaceUp(position)) {
            //Alert the user of an invalid move
            Snackbar.make(clRoot, "Invalid move!!", Snackbar.LENGTH_SHORT).show()
            return
        }

        //Actually flip over the card
        if(memoryGame.flipCard(position)) {
            Log.i(TAG, "Found a match ! Num of pairs found: ${memoryGame.numPairsFound} ")
        }
        adapter.notifyDataSetChanged()
    }
}