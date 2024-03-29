package com.app.scavenger;

import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public abstract class EndlessRecyclerViewScrollListener extends RecyclerView.OnScrollListener {

    // The current offset index of data you have loaded
    private int currentPage = 0;
    // The total number of items in the dataset after the last load
    private int previousTotalItemCount = 0;
    // True if we are still waiting for the last set of data to load.
    private boolean loading = true;
    // Sets the starting page index
    private static final int startingPageIndex = 0;

    private final ProgressBar mProgressBar;
    private final ConnectionDetector con;
    
    final RecyclerView.LayoutManager mLayoutManager;

    public EndlessRecyclerViewScrollListener(LinearLayoutManager layoutManager, ProgressBar progressBar, ConnectionDetector con) {
        this.mLayoutManager = layoutManager;
        this.mProgressBar = progressBar;
        this.con = con;
    }

    public int getLastVisibleItem(int[] lastVisibleItemPositions) {
        int maxSize = 0;
        for (int i : lastVisibleItemPositions) {
            if (i == 0) {
                maxSize = lastVisibleItemPositions[i];
            }
            else if (lastVisibleItemPositions[i] > maxSize) {
                maxSize = lastVisibleItemPositions[i];
            }
        }
        return maxSize;
    }

    // This happens many times a second during a scroll, so be wary of the code you place here.
    // We are given a few useful parameters to help us work out if we need to load some more data,
    // but first we check if we are waiting for the previous load to finish.
    @Override
    public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
        int lastVisibleItemPosition = 0;
        int totalItemCount = mLayoutManager.getItemCount();

        if (con.connectedToInternet()) {
            if (mLayoutManager instanceof StaggeredGridLayoutManager) {
                int[] lastVisibleItemPositions = ((StaggeredGridLayoutManager) mLayoutManager).findLastVisibleItemPositions(null);
                // get maximum element within the list
                lastVisibleItemPosition = getLastVisibleItem(lastVisibleItemPositions);
            } else if (mLayoutManager instanceof GridLayoutManager) {
                lastVisibleItemPosition = ((GridLayoutManager) mLayoutManager).findLastVisibleItemPosition();
            } else if (mLayoutManager instanceof LinearLayoutManager) {
                lastVisibleItemPosition = ((LinearLayoutManager) mLayoutManager).findLastVisibleItemPosition();
            }

            // If the total item count is zero and the previous isn't, assume the
            // list is invalidated and should be reset back to initial state
            if (totalItemCount < previousTotalItemCount) {
                this.currentPage = startingPageIndex;
                mProgressBar.setVisibility(View.GONE);
                this.previousTotalItemCount = totalItemCount;
                if (totalItemCount == 0) {
                    this.loading = true;
                }
            }
            // If it’s still loading, we check to see if the dataset count has
            // changed, if so we conclude it has finished loading and update the current page
            // number and total item count.
            if (loading && (totalItemCount > previousTotalItemCount)) {
                loading = false;
                mProgressBar.setVisibility(View.GONE);
                previousTotalItemCount = totalItemCount;
            }

            // If it isn’t currently loading, we check to see if we have breached
            // the visibleThreshold and need to reload more data.
            // If we do need to reload some more data, we execute onLoadMore to fetch the data.
            // threshold should reflect how many total columns there are too
            // The minimum amount of items to have below your current scroll position
            // before loading more.
            int visibleThreshold = 4;
            if (!loading && (lastVisibleItemPosition + visibleThreshold) > totalItemCount && totalItemCount >= 9/* && currentPage < 5*/) {
                currentPage++;
                mProgressBar.setVisibility(View.VISIBLE);
                onLoadMore(currentPage, totalItemCount, view);
                loading = true;
                isLoading();
            }
        }
    }

    // Gives a little bit of a delay before removing the Progress Bar
    public void isLoading() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {

            mProgressBar.setVisibility(View.GONE);
            loading = false;

        }, 1000);
    }

    // Call this method whenever performing new searches
    public void resetState() {
        this.currentPage = startingPageIndex;
        this.previousTotalItemCount = 0;
        this.loading = true;
    }

    // Defines the process for actually loading more data based on page
    public abstract void onLoadMore(int page, int totalItemsCount, RecyclerView view);


}
