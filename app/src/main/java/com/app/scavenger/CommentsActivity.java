package com.app.scavenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class CommentsActivity extends AppCompatActivity {

    private TextView mPostComment, mRecipeName, mRecipeSource;
    private EditText mCommentEditText;
    private ConstraintLayout mParentLayout;

    private String recipeName, recipeSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        mPostComment = findViewById(R.id.post_textButton);
        mCommentEditText = findViewById(R.id.comment_editText);
        mParentLayout = findViewById(R.id.parent_layout);
        mRecipeName = findViewById(R.id.comment_recipeName);
        mRecipeSource = findViewById(R.id.comment_recipeSource);

        if (getIntent().getExtras() != null) {

            recipeName = getIntent().getExtras().getString("recipe_name");
            recipeSource = getIntent().getExtras().getString("recipe_source");
            boolean focus = getIntent().getExtras().getBoolean("focus", false);

            if (focus) {
                mCommentEditText.requestFocus();
            } else {
                mParentLayout.requestFocus();
            }
        }

        mRecipeName.setText(recipeName);
        mRecipeSource.setText(recipeSource);

        ImageButton backButton = findViewById(R.id.comments_back);
        backButton.setOnClickListener(v -> finish());
    }
}