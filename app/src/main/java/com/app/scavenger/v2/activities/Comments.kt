package com.app.scavenger.v2.activities

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.scavenger.*
import com.app.scavenger.databinding.ActivityCommentsBinding
import com.app.scavenger.dialog.DialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import java.util.*
import kotlin.collections.HashMap

class Comments: AppCompatActivity() {

    private lateinit var binding: ActivityCommentsBinding
    private lateinit var recipeName: String
    private lateinit var recipeSource: String
    private lateinit var userName: String
    private lateinit var connection: ConnectionDetector
    private var recipeId: Int = 0
    private var hasFocus: Boolean = false

    private val commentItems = mutableListOf<CommentItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        binding = ActivityCommentsBinding.inflate(layoutInflater)
        connection = ConnectionDetector(this)

        recipeName = intent.extras?.getString("recipe_name").toString()
        recipeSource = intent.extras?.getString("recipe_source").toString()
        recipeId = intent.extras?.getInt("recipe_id") ?: 0
        hasFocus = intent.extras?.getBoolean("focus", false) ?: false

        if (hasFocus) {
            if (BaseApplication.instance.isUserLoggedIn()) {
                binding.commentEditText.requestFocus()
            } else {
                binding.parentLayout.requestFocus()
            }
        } else {
            binding.parentLayout.requestFocus()
        }

        binding.commentRecipeName.text = recipeName
        binding.commentRecipeSource.text = recipeSource

        retrieveNameFromFirebase()
        updateViews()

        binding.commentsRecyclerview.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))

        binding.commentsRetryConnection.setOnClickListener {
            checkIfConnected()
        }

        binding.commentEditText.addTextChangedListener {
            object : TextWatcher {

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    if (p0?.toString()?.trim()?.length != 0) {
                        binding.postTextButton.isEnabled = true
                        binding.postTextButton.setTextColor(Color.BLUE)
                    } else {
                        binding.postTextButton.isEnabled = false
                        binding.postTextButton.setTextColor(Color.GRAY)
                    }
                    binding.postTextButton.setBackgroundColor(Color.WHITE)
                }

                override fun afterTextChanged(p0: Editable?) {}
            }
        }

        binding.postTextButton.setOnClickListener {
            if (binding.commentEditText.text.isEmpty()) {
                if (connection.connectedToInternet()) {
                    postComment()
                } else {
                    DialogBuilder.showInformationDialog(this,
                    Constants.noInternetTitle,
                    Constants.noInternetMessage)
                }
            }
        }
    }

    private fun postComment() {
        val commentItem = CommentItem()
        commentItem.userId = BaseApplication.instance.getUserId()
        commentItem.name = userName
        commentItem.detail = binding.commentEditText.text.toString()

        binding.commentEditText.setText("")
        binding.commentEditText.clearFocus()

        commentItems.add(0, commentItem)

        val commentsAdapter = CommentsAdapter(this, commentItems, recipeId, recipeName, recipeSource)
        binding.commentsRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.commentsRecyclerview.adapter = commentsAdapter

        sendCommentToFirebase(commentItem)
    }

    private fun sendCommentToFirebase(commentItem: CommentItem) {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val year = calendar.get(Calendar.YEAR)

        calendar.clear()
        calendar.set(year, month, day)

        val timestamp = Timestamp(Date())

        val commentInfo = HashMap<String, Any>()

        val commentReference = BaseApplication.instance.getFirebaseDatabase()
            .collection(Constants.firebaseComments)
            .document(recipeId.toString())
            .collection(Constants.COMMENTS)

        commentInfo[Constants.firebaseTime] = timestamp
        commentInfo[Constants.COMMENT_USERID] = commentItem.userId
        commentInfo[Constants.COMMENT_NAME] = commentItem.name
        commentInfo[Constants.COMMENT_DETAIL] = commentItem.detail

        commentReference.document().set(commentInfo)
            .addOnSuccessListener { Log.d(BaseApplication.TAG, "Comment saved to Firebase") }
            .addOnFailureListener {
                Toast.makeText(this, "Error posting comment. Please check connection and try again", Toast.LENGTH_SHORT).show()
                Log.d(BaseApplication.TAG, it.toString())
            }
    }

    private fun checkIfConnected() {
        if (connection.connectedToInternet()) {
            binding.commentsLoading.visibility = View.VISIBLE
            binding.commentsRetryConnection.visibility = View.GONE
            binding.notConnectedTextComments.visibility = View.GONE
            retrieveCommentsFromFirebase()
        }
    }

    private fun updateViews() {
        if (!BaseApplication.instance.isUserLoggedIn()) {
            binding.postTextButton.visibility = View.GONE
            binding.commentEditText.visibility = View.GONE
            binding.notSignedText.visibility = View.VISIBLE
        } else {
            binding.notSignedText.visibility = View.GONE
        }

        if (connection.connectedToInternet()) {
            binding.notConnectedTextComments.visibility = View.GONE
            binding.commentsRetryConnection.visibility = View.GONE
            retrieveCommentsFromFirebase()
        } else {
            binding.commentsLoading.visibility = View.GONE
            binding.commentsRetryConnection.visibility = View.VISIBLE
            binding.notConnectedTextComments.visibility = View.VISIBLE
        }
    }

    private fun retrieveCommentsFromFirebase() {
        binding.commentsLoading.visibility = View.VISIBLE
        val commentsRef = BaseApplication.instance.getFirebaseDatabase()
            .collection(Constants.firebaseComments).document(recipeId.toString())
            .collection(Constants.COMMENTS)

        commentsRef.orderBy(Constants.firebaseTime, Query.Direction.DESCENDING).get()
            .addOnSuccessListener {
                if (it.isEmpty) {
                    binding.commentsRecyclerview.adapter = null
                } else {
                    for (document in it) {
                        val commentItem = CommentItem()
                        commentItem.name = document.getString(Constants.COMMENT_NAME)
                        commentItem.detail = document.getString(Constants.COMMENT_DETAIL)
                        commentItem.userId = document.getString(Constants.COMMENT_USERID)

                        commentItems.add(commentItem)
                    }

                    val commentsAdapter = CommentsAdapter(this, commentItems, recipeId, recipeName, recipeSource)
                    binding.commentsRecyclerview.layoutManager = LinearLayoutManager(this)
                    binding.commentsRecyclerview.adapter = commentsAdapter
                }
                binding.commentsLoading.visibility = View.GONE
            }
    }

    private fun retrieveNameFromFirebase() {
        val userRef = BaseApplication.instance.getFirebaseDatabase()
            .collection(Constants.firebaseUser).document(BaseApplication.instance.getUserId())
        userRef.get().addOnCompleteListener {
            if (it.isSuccessful) {
                userName = it.result?.getString("name") ?: "Anonymous"
            } else {
                Log.d(BaseApplication.TAG, "get failed with ${it.exception}")
            }
        }
    }
}