package com.mono.dashboard;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * This listener class is used to detect changes in the scrolling behavior of the recycler view.
 *
 * @author Gary Ng
 */
public abstract class SimpleOnScrollListener extends RecyclerView.OnScrollListener {

    private int threshold;

    public SimpleOnScrollListener(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        if (canScroll()) {
            return;
        }

        LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();

        if (dy < 0) {
            if (manager.findFirstVisibleItemPosition() <= threshold) {
                onScrolledTop();
            }
        } else if (dy > 0) {
            int threshold = Math.max(getCount() - this.threshold, 0);

            if (manager.findLastVisibleItemPosition() >= threshold) {
                onScrolledBottom();
            }
        }
    }

    public boolean canScroll() {
        return true;
    }

    public abstract void onScrolledTop();

    public abstract void onScrolledBottom();

    public abstract int getCount();
}
