package com.mono.contacts;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mono.R;
import com.mono.util.BitmapHelper;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.Pixels;
import com.mono.util.SimpleClickableView;
import com.mono.util.SimpleDataSource;
import com.mono.util.SimpleViewHolder;
import com.mono.util.SimpleViewHolder.HolderItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A adapter used to display contacts in the recycler view.
 *
 * @author Gary Ng
 */
public class ContactsAdapter extends RecyclerView.Adapter<SimpleViewHolder> {

    public static final int TYPE_LABEL = 0;
    public static final int TYPE_CONTACT = 1;

    private static final int ICON_DIMENSION_DP = 40;
    private static final int RADIUS_DP = 2;

    private ContactsAdapterListener listener;
    private List<ContactsGroup> groups = new ArrayList<>();

    private boolean showEmptyLabels = true;
    private String[] terms;

    public ContactsAdapter(ContactsAdapterListener listener) {
        this.listener = listener;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        SimpleViewHolder holder = null;

        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == TYPE_LABEL) {
            View view = inflater.inflate(R.layout.contacts_label, parent, false);
            holder = new LabelHolder(view);
        } else if (viewType == TYPE_CONTACT) {
            SimpleClickableView container = new SimpleClickableView(context);

            View view = inflater.inflate(R.layout.contacts_item, container, false);
            container.addView(view);

            holder = new ContactHolder(container);
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
        HolderItem item = getItem(position);
        if (item == null) {
            return;
        }

        holder.onBind(item);

        if (holder instanceof ContactHolder) {
            int color = Colors.getColor(holder.itemView.getContext(), R.color.red);
            ((ContactHolder) holder).onHighlight(item, terms, color);
        }
    }

    public HolderItem getItem(int position) {
        HolderItem item = null;

        int size = 0;
        for (ContactsGroup group : groups) {
            int count = group.getCount();

            if (position >= size && position < size + count) {
                item = group.getItem(position - size);
                break;
            }

            size += count;
        }

        return item;
    }

    @Override
    public int getItemViewType(int position) {
        int viewType = -1;

        int size = 0;
        for (ContactsGroup group : groups) {
            int count = group.getCount();

            if (position >= size && position < size + count) {
                HolderItem item = group.getItem(position - size);
                if (item instanceof LabelItem) {
                    viewType = TYPE_LABEL;
                } else {
                    viewType = TYPE_CONTACT;
                }
                break;
            }

            size += count;
        }

        return viewType;
    }

    @Override
    public int getItemCount() {
        int count = 0;

        for (ContactsGroup group : groups) {
            count += group.getCount();
        }

        return count;
    }

    @Override
    public void onViewRecycled(SimpleViewHolder holder) {
        holder.onViewRecycled();
    }

    /**
     * Add contacts group to this adapter.
     *
     * @param group The instance of the contacts group.
     */
    public void add(ContactsGroup group) {
        groups.add(group);
        notifyDataSetChanged();
    }

    /**
     * Retrieve the exact adapter position while factoring the position of the labels.
     *
     * @param position The position to be resolved.
     * @return the adapter position.
     */
    public int getAdapterPosition(int position) {
        int offset = 0, size = 0;

        for (ContactsGroup group : groups) {
            offset += group.getContactsOffset();

            size += group.getCount() - group.getContactsOffset();
            if (size > position) {
                break;
            }
        }

        return offset + position;
    }

    /**
     * Notify the adapter the item has been added as well as refresh the label belonging to its
     * group.
     *
     * @param position The position in the adapter.
     */
    public void notifyInserted(int position) {
        notifyItemInserted(position);

        setShowEmptyLabels(showEmptyLabels, false);
        notifyItemChanged(getGroupLabelPosition(position));
    }

    /**
     * Notify the adapter the item has been moved as well as refresh the label belonging to its
     * group.
     *
     * @param fromPosition The original position in the adapter.
     * @param toPosition The target position in the adapter.
     */
    public void notifyMoved(int fromPosition, int toPosition) {
        notifyItemMoved(fromPosition, toPosition);

        if (fromPosition > toPosition) {
            toPosition--;
        } else if (fromPosition < toPosition) {
            fromPosition--;
        }

        setShowEmptyLabels(showEmptyLabels, false);
        notifyItemChanged(getGroupLabelPosition(fromPosition));
        notifyItemChanged(getGroupLabelPosition(toPosition));
    }

    /**
     * Notify the adapter the item has been removed as well as refresh the label belonging to its
     * group.
     *
     * @param position The position in the adapter.
     */
    public void notifyRemoved(int position) {
        notifyItemRemoved(position);

        setShowEmptyLabels(showEmptyLabels, false);
        notifyItemChanged(getGroupLabelPosition(position));
    }

    /**
     * Display labels even if there are no contacts belonging to the group.
     *
     * @param status The value of the status.
     * @param notify The value to notify the adapter to refresh.
     */
    public void setShowEmptyLabels(boolean status, boolean notify) {
        showEmptyLabels = status;

        for (ContactsGroup group : groups) {
            group.setLabelHidden(!showEmptyLabels & group.isEmpty());
        }

        if (notify) {
            notifyDataSetChanged();
        }
    }

    /**
     * Set terms to highlight within the items when being displayed.
     *
     * @param terms The terms to highlight.
     */
    public void setHighlightTerms(String[] terms) {
        this.terms = terms;
    }

    /**
     * Retrieve the position of the group label item in the adapter.
     *
     * @param adapterPosition The position in the adapter.
     * @return the position of the label.
     */
    private int getGroupLabelPosition(int adapterPosition) {
        int result = -1;

        int size = 0;
        for (ContactsGroup group : groups) {
            int count = group.getCount();

            if (Common.between(adapterPosition, size, size + count - 1)) {
                result = size;
                break;
            }

            size += count;
        }

        return result;
    }

    /**
     * Retrieve the offset of where the contacts of the specific group begin.
     *
     * @param groupId The group category.
     * @return the offset of the starting position.
     */
    private int getGroupOffset(int groupId) {
        int offset = -1;

        int size = 0;
        for (ContactsGroup group : groups) {
            int count = group.getCount();

            if (group.id == groupId) {
                offset = size;
                break;
            }

            size += count;
        }

        return offset;
    }

    /**
     * Retrieve the group and position at the specific group.
     *
     * @param adapterPosition The position in the adapter.
     * @return an array consisting of the group and position.
     */
    private int[] getGroupPosition(int adapterPosition) {
        int[] result = null;

        int size = 0;
        for (ContactsGroup group : groups) {
            int count = group.getCount();

            if (Common.between(adapterPosition, size, size + count - 1)) {
                result = new int[]{group.id, adapterPosition - size - 1};
                break;
            }

            size += count;
        }

        return result;
    }

    /**
     * Display progress spinner next to the label.
     *
     * @param group The group category.
     * @param visible The visible status of the spinner.
     */
    public void setGroupProgress(int group, boolean visible) {
        int position = getGroupOffset(group);
        if (position == -1) {
            return;
        }

        LabelItem item = (LabelItem) getItem(position);
        item.showProgress = visible;

        notifyItemChanged(position);
    }

    public static class LabelItem extends HolderItem {

        public int labelResId;
        public String count;
        public boolean showProgress;

        public LabelItem(int labelResId) {
            this.labelResId = labelResId;
        }
    }

    public class LabelHolder extends SimpleViewHolder {

        private TextView label;
        private TextView count;
        private ProgressBar progress;

        public LabelHolder(View itemView) {
            super(itemView);

            label = (TextView) itemView.findViewById(R.id.label);
            count = (TextView) itemView.findViewById(R.id.count);
            progress = (ProgressBar) itemView.findViewById(R.id.progress);
        }

        @Override
        public void onBind(HolderItem holderItem) {
            LabelItem item = (LabelItem) holderItem;

            label.setText(item.labelResId);
            count.setText(item.count);
            progress.setVisibility(item.showProgress ? View.VISIBLE : View.GONE);
        }
    }

    public static class ContactItem extends HolderItem {

        public int iconResId;
        public int iconColor;
        public byte[] iconBytes;
        public Bitmap iconBitmap;
        public CharSequence name;
        public CharSequence msg;
        public int msgColor;

        public ContactItem(String id) {
            this.id = id;
        }
    }

    public class ContactHolder extends SimpleViewHolder {

        private ImageView icon;
        private TextView name;
        private TextView msg;

        private AsyncTask<Object, Void, Bitmap> task;

        public ContactHolder(View itemView) {
            super(itemView);

            icon = (ImageView) itemView.findViewById(R.id.icon);
            name = (TextView) itemView.findViewById(R.id.name);
            msg = (TextView) itemView.findViewById(R.id.msg);

            if (listener != null) {
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int[] result = getGroupPosition(getAdapterPosition());

                        if (result != null) {
                            int group = result[0], position = result[1];
                            listener.onClick(view, group, position);
                        }
                    }
                });
            }
        }

        @Override
        public void onBind(HolderItem holderItem) {
            ContactItem item = (ContactItem) holderItem;

            if (task != null) {
                task.cancel(true);
                task = null;
            }

            if (item.iconBitmap != null) {
                icon.setImageBitmap(item.iconBitmap);
                icon.clearColorFilter();
            } else {
                icon.setImageResource(item.iconResId);
                icon.setColorFilter(item.iconColor | 0xFF000000);

                if (item.iconBytes != null) {
                    createImageTask(item);
                }
            }

            name.setText(item.name);

            msg.setText(item.msg);
            msg.setTextColor(item.msgColor);
        }

        public void onHighlight(HolderItem holderItem, String[] terms, int color) {
            ContactItem item = (ContactItem) holderItem;

            name.setText(Common.highlight(item.name.toString(), terms, color));
            msg.setText(Common.highlight(item.msg.toString(), terms, color));
        }

        private void createImageTask(ContactItem item) {
            task = new AsyncTask<Object, Void, Bitmap>() {

                private ContactItem item;

                @Override
                protected Bitmap doInBackground(Object... params) {
                    Context context = (Context) params[0];
                    item = (ContactItem) params[1];

                    int dimension = Pixels.pxFromDp(context, ICON_DIMENSION_DP);
                    Bitmap bitmap = BitmapHelper.createBitmap(item.iconBytes, dimension, dimension);

                    int color = Colors.getColor(context, R.color.colorPrimary);
                    int radius = Pixels.pxFromDp(context, RADIUS_DP);

                    return BitmapHelper.createCircleBitmap(bitmap, color, radius);
                }

                @Override
                protected void onPostExecute(Bitmap result) {
                    if (result != null) {
                        item.iconBitmap = result;

                        icon.setImageBitmap(result);
                        icon.clearColorFilter();
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, itemView.getContext(), item);
        }
    }

    public static class ContactsGroup implements SimpleDataSource<HolderItem> {

        private int id;
        private final SimpleDataSource<HolderItem> dataSource;

        private LabelItem labelItem;
        private boolean isLabelHidden;

        public ContactsGroup(int id, int labelResId, SimpleDataSource<HolderItem> dataSource) {
            this.id = id;
            labelItem = new LabelItem(labelResId);
            this.dataSource = dataSource;
        }

        @Override
        public HolderItem getItem(int position) {
            HolderItem item;

            if (position == 0 && !isLabelHidden) {
                labelItem.count = String.format("(%d)", dataSource.getCount());
                item = labelItem;
            } else {
                item = dataSource.getItem(position - getContactsOffset());
            }

            return item;
        }

        @Override
        public int getCount() {
            return getContactsOffset() + dataSource.getCount();
        }

        public boolean isEmpty() {
            return dataSource.getCount() == 0;
        }

        public int getContactsOffset() {
            return !isLabelHidden ? 1 : 0;
        }

        public void setLabelHidden(boolean hidden) {
            isLabelHidden = hidden;
        }
    }

    public interface ContactsAdapterListener {

        void onClick(View view, int group, int position);
    }
}
