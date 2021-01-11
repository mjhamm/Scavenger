package com.app.scavenger;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TopToolbar extends LinearLayout {

    private TextView title;

    public TopToolbar(Context context) {
        super(context);
        initializeViews(context);
    }

    public TopToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public TopToolbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeViews(context);
    }

    private void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_top_toolbar, this);
    }

    public void setTitle(String title) {
        this.title.setText(title);
    }

    private void setBackButton(Context context) {
        ((Activity) context).finish();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        title = findViewById(R.id.title);
        ImageButton backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> setBackButton(getContext()));
    }
}
