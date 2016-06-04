package com.mono.util;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public abstract class SimpleViewHolder extends RecyclerView.ViewHolder {

    public SimpleViewHolder(View itemView) {
        super(itemView);
    }

    public abstract void onBind(HolderItem holderItem);

    public void onViewRecycled() {

    }

    public static abstract class HolderItem {

        public String id;

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
