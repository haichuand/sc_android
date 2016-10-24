package com.mono.util;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * This holder class is used to provide an abstract class representing a view holder with
 * additional data commonly used by their implementations.
 *
 * @author Gary Ng
 */
public abstract class SimpleViewHolder extends RecyclerView.ViewHolder {

    public SimpleViewHolder(View itemView) {
        super(itemView);
    }

    public abstract void onBind(HolderItem holderItem);

    public void onViewRecycled() {

    }

    public static abstract class HolderItem {

        public String id;
        public String sortValue;

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof HolderItem)) {
                return false;
            }

            HolderItem item = (HolderItem) object;

            if (!Common.compareStrings(id, item.id)) {
                return false;
            }

            return true;
        }
    }
}
