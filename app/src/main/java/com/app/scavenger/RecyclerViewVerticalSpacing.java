package com.app.scavenger;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewVerticalSpacing extends RecyclerView.ItemDecoration {

    private final int bottomSpaceHeight;
    private final int topSpaceHeight;

    public RecyclerViewVerticalSpacing(int topSpaceHeight, int bottomSpaceHeight) {
        this.topSpaceHeight = topSpaceHeight;
        this.bottomSpaceHeight = bottomSpaceHeight;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        outRect.bottom = bottomSpaceHeight;
        outRect.top = topSpaceHeight;
    }

}
