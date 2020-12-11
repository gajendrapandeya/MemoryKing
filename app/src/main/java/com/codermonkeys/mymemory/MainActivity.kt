package com.codermonkeys.mymemory

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.codermonkeys.mymemory.models.BoardSize
import com.codermonkeys.mymemory.models.BoardSize.EASY
import com.codermonkeys.mymemory.models.MemoryGame
import com.codermonkeys.mymemory.models.UserImageList
import com.codermonkeys.mymemory.utils.EXTRA_BOARD_SIZE
import com.codermonkeys.mymemory.utils.EXTRA_GAME_NAME
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import nl.dionsegijn.konfetti.KonfettiView
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size


@Suppress("CAST_NEVER_SUCCEEDS")
class MainActivity : AppCompatActivity() {


    companion object {
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 18
    }

    private lateinit var clRoot: CoordinatorLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumMovesLeft: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var viewConfetti: KonfettiView

    private var gameName: String? = null
    private val db = Firebase.firestore
    private var customGameImages: List<String>? = null
    private lateinit var adapter: MemoryBoardAdapter
    private lateinit var memoryGame: MemoryGame
    private var boardSize = EASY
    private lateinit var radioGroupSize: RadioGroup
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var mInterstitialAd: InterstitialAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MyMemory)
        setContentView(R.layout.activity_main)

        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)
        tvNumMovesLeft = findViewById(R.id.tvNumMovesLeft)
        viewConfetti = findViewById(R.id.viewKonfetti)

        // Obtain the FirebaseAnalytics instance.
        firebaseAnalytics = Firebase.analytics

        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = "ca-app-pub-3940256099942544/1033173712"
        mInterstitialAd.loadAd(AdRequest.Builder().build())

        setUpBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                //Setup the game again
                if (memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()) {
                    showAlertDialog("Are you sure to Quit the game?", null) {
                        setUpBoard()
                    }
                } else {
                    setUpBoard()
                }
                return true
            }

            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }

            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }

            R.id.mi_download -> {
                showDownloadDialog()
                return true
            }

            R.id.mi_available_games -> {
                showDialog()
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_REQUEST_CODE && resultCode == RESULT_OK) {
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGameName == null) {
                Log.e(TAG, "Got null custom game name from CreateActivity: ")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    @SuppressLint("SetTextI18n")
    private fun movesLeft() {
        if (memoryGame.getNumMoves() > boardSize.getNoOfMovesLeft()) {
            if (!memoryGame.haveWonGame())
                showUnCancelableAlertDialog()
        } else {
            tvNumMovesLeft.text = "Moves Left: ${memoryGame.getNumMovesLeft()}"
        }
    }

    private fun showDownloadDialog() {
        val boardDownloadView = LayoutInflater.from(this).inflate(
            R.layout.dialog_download_board,
            null
        )
        showAlertDialog("Fetch Memory Game", boardDownloadView) {
            //Grab the text name that the user wants to download
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            if (gameToDownload.isNotBlank()) {
                downloadGame(gameToDownload)
            } else {
                Snackbar.make(clRoot, "Please enter game name", Snackbar.LENGTH_SHORT).show()
            }

        }
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            Log.i(TAG, "downloadGame: ")
            val userImageList = document.toObject(UserImageList::class.java)
            if (userImageList?.images == null) {
                Log.e(TAG, "Invalid custom game data from FireStore ")
                Snackbar.make(
                    clRoot,
                    "Sorry, we couldn't find any such game '$customGameName'",
                    Snackbar.LENGTH_SHORT
                ).show()
                return@addOnSuccessListener
            }
            val numCards = userImageList.images.size * 2
            boardSize = BoardSize.getByValue(numCards)
            customGameImages = userImageList.images
            for (imageUrl in userImageList.images) {
                Glide.with(this).load(imageUrl).preload()
            }
            Snackbar.make(clRoot, "You're now playing '$customGameName'", Snackbar.LENGTH_LONG)
                .show()
            gameName = customGameName
            setUpBoard()
        }.addOnFailureListener {
            Log.e(TAG, "Exception when retrieving game ", it)
        }
    }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)

        showAlertDialog("Create your own memory board", boardSizeView, View.OnClickListener {
            //set a new value for the board size
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            //Navigate to a new activity
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        })
    }

    @SuppressLint("InflateParams")
    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        radioGroupSize = boardSizeView.findViewById(R.id.radioGroup)

        when (boardSize) {
            EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }

        showAlertDialog("Choose new size", boardSizeView) {
            //set a new value for the board size
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            setUpBoard()
        }
    }

    private fun showAlertDialog(
        title: String,
        view: View?,
        positiveClickListener: View.OnClickListener
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK") { _, _ ->
                positiveClickListener.onClick(null)
            }.show()
    }

    @SuppressLint("SetTextI18n")
    private fun setUpBoard() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        when (boardSize) {
            EASY -> {
                tvNumMoves.text = "Easy: 4 x 2"
                tvNumPairs.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "Medium: 6 x 3"
                tvNumPairs.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text = "Hard: 6 x 4"
                tvNumPairs.text = "Pairs: 0 / 12"
            }
        }

        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
        memoryGame = MemoryGame(boardSize, customGameImages)
        tvNumMovesLeft.text = "Moves Left: ${memoryGame.getNumMovesLeft()}"
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

    @SuppressLint("SetTextI18n")
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
        if (memoryGame.flipCard(position)) {
            Log.i(TAG, "Found a match ! Num of pairs found: ${memoryGame.numPairsFound} ")
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full)
            ) as Int
            tvNumPairs.setTextColor(color)
            tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"

            if (memoryGame.haveWonGame()) {
                Snackbar.make(clRoot, "You Won. Congratulations!!", Snackbar.LENGTH_SHORT).show()
                showConfetti()
            }
        }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        //Logic for Moves Left
        movesLeft()
        adapter.notifyDataSetChanged()
    }

    private fun showUnCancelableAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Game Over!!. Better luck next time")
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                if (mInterstitialAd.isLoaded) {
                    mInterstitialAd.show()
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.")
                    gameName = null
                    customGameImages = null
                    setUpBoard()
                }

                mInterstitialAd.adListener = object : AdListener() {
                    override fun onAdClosed() {
                        gameName = null
                        customGameImages = null
                        setUpBoard()
                        mInterstitialAd.loadAd(AdRequest.Builder().build())
                    }
                }

            }
        val alert = builder.create()
        alert.show()
    }


    private fun showAd() {
        if (mInterstitialAd.isLoaded) {
            mInterstitialAd.show()
        } else {
            Log.d("TAG", "The interstitial wasn't loaded yet.")
        }
    }

    private fun showConfetti() {
        viewConfetti.build()
            .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
            .setDirection(0.0, 359.0)
            .setSpeed(1f, 5f)
            .setFadeOutEnabled(true)
            .setTimeToLive(2000L)
            .addShapes(Shape.RECT, Shape.CIRCLE)
            .addSizes(Size(12))
            .setPosition(-50f, viewConfetti.width + 50f, -50f, -50f)
            .streamFor(200, 2000L)
    }

    private fun createListView() {
        val linearLayout = LinearLayout(this)
        val listView = ListView(this)
        val gameNames = arrayListOf<String>()

        val adapter = ArrayAdapter(
            this@MainActivity,
            android.R.layout.simple_list_item_1,
            gameNames
        )

        gameNames.clear()
        db.collection("games").get().addOnSuccessListener {
            if (it.isEmpty) {
                Log.i(TAG, "No data found")
            } else {
                for (docs in it) {
                    gameNames.add(docs.id)
                }

            }
        }
        listView.adapter = adapter
        linearLayout.addView(listView)
        this.setContentView(
            linearLayout, LinearLayout.LayoutParams(
                MATCH_PARENT, WRAP_CONTENT
            )
        )
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Available Game Names")
        val gameNames: MutableList<String> = ArrayList()
        db.collection("games").get().addOnSuccessListener {
            if (it.isEmpty) {
                Log.i(TAG, "No data found")
            } else {
                for (docs in it) {
                    gameNames.add(docs.id)
                }
                val dataAdapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line, gameNames
                )
                builder.setAdapter(
                    dataAdapter
                ) { _, _ ->
                }
                val dialog = builder.create()
                dialog.show()
                Log.d(TAG, "insideloop: $gameNames")
            }
        }
    }
}