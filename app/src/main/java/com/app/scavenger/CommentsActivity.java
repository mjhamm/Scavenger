package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class CommentsActivity extends AppCompatActivity {

    private TextView mPostComment;
    private TextView mNotConnectedText;
    private EditText mCommentEditText;
    private CommentsAdapter commentsAdapter;
    private RecyclerView mCommentsRecyclerView;
    private FrameLayout mLoadingLayout;
    private ConnectionDetector con;
    private MaterialButton mReloadComments;

    private List<CommentItem> commentItems;
    private LinearLayoutManager mLayoutManager;

    private String userId, userName, recipeName, recipeSource;
    private int recipeId;
    private boolean logged;
    private SharedPreferences sharedPreferences;

    // Database
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        mPostComment = findViewById(R.id.post_textButton);
        mCommentEditText = findViewById(R.id.comment_editText);
        ConstraintLayout mParentLayout = findViewById(R.id.parent_layout);
        TextView mRecipeName = findViewById(R.id.comment_recipeName);
        TextView mRecipeSource = findViewById(R.id.comment_recipeSource);
        TextView mNotSignedText = findViewById(R.id.not_signed_text);
        mCommentsRecyclerView = findViewById(R.id.comments_recyclerview);
        mLoadingLayout = findViewById(R.id.comments_loading);
        mNotConnectedText = findViewById(R.id.not_connected_text_comments);
        mReloadComments = findViewById(R.id.comments_retry_connection);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        con = new ConnectionDetector(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        commentItems = new ArrayList<>();

        getInfoFromSharedPrefs();

        TopToolbar topToolbar = findViewById(R.id.comments_toolbar);
        topToolbar.setTitle("Comments");

        mLoadingLayout.setVisibility(View.VISIBLE);

        if (getIntent().getExtras() != null) {

            recipeName = getIntent().getExtras().getString("recipe_name");
            recipeSource = getIntent().getExtras().getString("recipe_source");
            recipeId = getIntent().getExtras().getInt("recipe_id");
            boolean focus = getIntent().getExtras().getBoolean("focus", false);

            if (focus) {
                if (logged) {
                    mCommentEditText.requestFocus();
                }
            } else {
                mParentLayout.requestFocus();
            }

        }

        mRecipeName.setText(recipeName);
        mRecipeSource.setText(recipeSource);

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
            userName = mAuth.getCurrentUser().getDisplayName();
        }

        if (!logged) {
            mPostComment.setVisibility(View.GONE);
            mCommentEditText.setVisibility(View.GONE);
            mNotSignedText.setVisibility(View.VISIBLE);
        } else {
            mNotSignedText.setVisibility(View.GONE);
        }

        mLayoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, mLayoutManager.getOrientation());
        mCommentsRecyclerView.addItemDecoration(dividerItemDecoration);

        if (con.connectedToInternet()) {
            mNotConnectedText.setVisibility(View.GONE);
            mReloadComments.setVisibility(View.GONE);
            retrieveCommentsFromFB(recipeId);
        } else {
            mLoadingLayout.setVisibility(View.GONE);
            mReloadComments.setVisibility(View.VISIBLE);
            mNotConnectedText.setVisibility(View.VISIBLE);
        }

        mReloadComments.setOnClickListener(v -> {
            if (con.connectedToInternet()) {
                mLoadingLayout.setVisibility(View.VISIBLE);
                mNotConnectedText.setVisibility(View.GONE);
                mReloadComments.setVisibility(View.GONE);
                retrieveCommentsFromFB(recipeId);
            }
        });

        mCommentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() != 0) {
                    mPostComment.setEnabled(true);
                    mPostComment.setTextColor(Color.BLUE);
                } else {
                    mPostComment.setEnabled(false);
                    mPostComment.setTextColor(Color.GRAY);
                }
                mPostComment.setBackgroundColor(Color.WHITE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mPostComment.setOnClickListener(v -> {
            // check if connected to the internet before posting

            if (!mCommentEditText.getText().toString().isEmpty()) {
                if (con.connectedToInternet()) {
                    postComment(userName, mCommentEditText.getText().toString());
                } else {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(Constants.noInternetTitle)
                            .setMessage(Constants.noInternetMessage)
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                }
            }

        });
    }

    private void postComment(String name, String detail) {

        // clear comment edittext
        mCommentEditText.setText("");
        mCommentEditText.clearFocus();
        // add comment to comment adapter

        CommentItem commentItem = new CommentItem();
        commentItem.setName(name);
        commentItem.setDetail(detail);

        commentItems.add(0,commentItem);

        commentsAdapter = new CommentsAdapter(this, commentItems, recipeId, recipeName, recipeSource);
        // show the comment in the recyclerview
        mCommentsRecyclerView.setLayoutManager(mLayoutManager);
        mCommentsRecyclerView.setAdapter(commentsAdapter);
        // send comment to Firebase along with user's name to display
        sendCommentToFB(recipeId, userId, commentItem);
        // send refresh to detail view to show comment under comments

    }

    private void sendCommentToFB(int recipeId, String userId, CommentItem commentItem) {
        // Send comment data to FB

        Calendar calendar = Calendar.getInstance();

        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR);

        calendar.clear();
        calendar.set(year, month, day);

        Date now = new Date();
        Timestamp timestamp = new Timestamp(now);

        HashMap<String, Object> commentInfo = new HashMap<>();

        CollectionReference commentReference = db.collection(Constants.firebaseComments).document(String.valueOf(recipeId)).collection("comments");

        commentInfo.put("Timestamp", timestamp);
        commentInfo.put("User ID", userId);
        commentInfo.put("name", commentItem.getName());
        commentInfo.put("detail", commentItem.getDetail());

        commentReference.document().set(commentInfo)
                .addOnSuccessListener(aVoid -> Log.d("CommentsActivity","Comment saved to Firebase"))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error posting comment. Please check connection and try again", Toast.LENGTH_SHORT).show();
                    Log.d("CommentsActivity", e.toString());
                });

    }

    private void retrieveCommentsFromFB(int recipeId) {

        mLoadingLayout.setVisibility(View.VISIBLE);
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        // reference to the users likes
        CollectionReference commentsRef = db.collection(Constants.firebaseComments).document(String.valueOf(recipeId)).collection("comments");
        // orders those likes by timestamp in descending order to show the most recent like on top
        commentsRef.orderBy(Constants.firebaseTime, Query.Direction.DESCENDING).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    String name, detail;

                    // if the number of likes the user has is 0
                    // set adapter for recycler to null
                    // this is so no possible overlap of another user can come through
                    if (queryDocumentSnapshots.isEmpty()) {
                        mCommentsRecyclerView.setAdapter(null);
                        // if the number of likes the user has is not 0
                        // clear the list and adapter
                    } else {
                        // go through each item in the snapshot from Firebase and set a new comment item with the information
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {

                            CommentItem commentItem = new CommentItem();
                            name = documentSnapshot.getString(Constants.COMMENT_ITEM);
                            detail = documentSnapshot.getString(Constants.COMMENT_DETAIL);

                            commentItem.setName(name);
                            commentItem.setDetail(detail);

                            commentItems.add(commentItem);
                        }
                        // create the adapter with the new list
                        commentsAdapter = new CommentsAdapter(this, commentItems, recipeId, recipeName, recipeSource);
                        // set adapter
                        mCommentsRecyclerView.setLayoutManager(mLayoutManager);
                        mCommentsRecyclerView.setAdapter(commentsAdapter);
                    }
                    mLoadingLayout.setVisibility(View.GONE);
                });
    }

    // Sets all variables related to logged status and user info
    private void getInfoFromSharedPrefs() {
        logged = sharedPreferences.getBoolean("logged", false);
    }

    @Override
    public void finish() {

        // Send a result back to the recipeitemscreen to let it know to recheck the comments
        Intent returnIntent = new Intent();
        //By not passing the intent in the result, the calling activity will get null data.
        setResult(RESULT_OK, returnIntent);
        super.finish();
    }
}