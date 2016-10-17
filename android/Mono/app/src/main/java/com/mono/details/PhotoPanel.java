package com.mono.details;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.mono.MediaManager;
import com.mono.R;
import com.mono.model.Event;
import com.mono.model.Media;
import com.mono.util.BitmapHelper;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.Pixels;
import com.mono.util.SimpleQuickAction;
import com.mono.util.UriHelper;

/**
 * This class is used to handle the photos section located in Event Details.
 *
 * @author Gary Ng
 */
public class PhotoPanel {

    private static final int PHOTO_WIDTH_DP = 120;
    private static final int PHOTO_HEIGHT_DP = 90;

    private static final String[] PHOTO_ACTIONS = {"View", "Remove"};
    private static final int PHOTO_ACTION_VIEW = 0;
    private static final int PHOTO_ACTION_REMOVE = 1;

    private EventDetailsActivity activity;
    private ViewGroup photos;

    private Event event;

    public PhotoPanel(EventDetailsActivity activity) {
        this.activity = activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        photos = (ViewGroup) activity.findViewById(R.id.photos);
    }

    /**
     * Initialize this panel using the given event.
     *
     * @param event The instance of the event.
     */
    public void setEvent(Event event) {
        this.event = event;

        photos.removeAllViews();
        createPhotoButton();

        if (!event.photos.isEmpty()) {
            for (Media photo : event.photos) {
                createPhoto(photo.uri, photo.thumbnail);
            }
        }
    }

    /**
     * Create a thumbnail from a byte array otherwise attempt to load it using the path given.
     * The resulting thumbnail will be appended to the photo section.
     *
     * @param uri The image path.
     * @param data The image data.
     */
    public void createPhoto(Uri uri, byte[] data) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.photos_item, null, false);

        ImageView image = (ImageView) view.findViewById(R.id.image);

        int width = Pixels.pxFromDp(activity, PHOTO_WIDTH_DP);
        int height = Pixels.pxFromDp(activity, PHOTO_HEIGHT_DP);

        Bitmap bitmap = null;
        if (data != null) {
            bitmap = BitmapHelper.createBitmap(data, width, height);
        } else if (Common.fileExists(uri.toString())) {
            bitmap = BitmapHelper.createBitmap(uri.toString(), width, height);
        }
        image.setImageBitmap(bitmap);

        int margin = Pixels.pxFromDp(activity, 2);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            Pixels.pxFromDp(activity, PHOTO_WIDTH_DP),
            Pixels.pxFromDp(activity, PHOTO_HEIGHT_DP)
        );
        params.setMargins(margin, margin, margin, margin);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPhotoClick(photos.indexOfChild(view));
            }
        });

        photos.addView(view, Math.max(photos.getChildCount() - 1, 0), params);
    }

    /**
     * Create a button to show photo picker to add images to event.
     */
    public void createPhotoButton() {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.photos_item, null, false);

        ImageView image = (ImageView) view.findViewById(R.id.image);
        image.setImageResource(R.drawable.ic_photo_add);

        int dimension = Pixels.pxFromDp(activity, PHOTO_HEIGHT_DP * 0.5f);
        RelativeLayout.LayoutParams imageParams =
            new RelativeLayout.LayoutParams(dimension, dimension);
        imageParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        image.setLayoutParams(imageParams);

        int color = Colors.getColor(activity, R.color.gray);
        image.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        int margin = Pixels.pxFromDp(activity, 2);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            Pixels.pxFromDp(activity, PHOTO_WIDTH_DP),
            Pixels.pxFromDp(activity, PHOTO_HEIGHT_DP)
        );
        params.setMargins(margin, margin, margin, margin);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPhotoPicker();
            }
        });

        photos.addView(view, params);
    }

    /**
     * Handle the action of clicking on the photo button. A popup of additional options will be
     * shown upon click.
     *
     * @param position The position of the photo.
     */
    public void onPhotoClick(int position) {
        final int photoPosition = position;

        View view = photos.getChildAt(position);

        SimpleQuickAction actionView = SimpleQuickAction.newInstance(activity);
        actionView.setColor(Colors.getColor(activity, R.color.colorPrimary));
        actionView.setActions(PHOTO_ACTIONS);

        int[] location = new int[2];
        view.getLocationInWindow(location);
        location[1] -= Pixels.Display.getStatusBarHeight(activity);
        location[1] -= Pixels.Display.getActionBarHeight(activity);

        int offsetX = location[0] + view.getWidth() / 2;
        int offsetY = view.getHeight();

        actionView.setPosition(location[0], location[1], offsetX, offsetY);
        actionView.setListener(new SimpleQuickAction.SimpleQuickActionListener() {
            @Override
            public void onActionClick(int position) {
                switch (position) {
                    case PHOTO_ACTION_VIEW:
                        Media photo = event.photos.get(photoPosition);
                        showPhotoViewer(photo);
                        break;
                    case PHOTO_ACTION_REMOVE:
                        event.photos.remove(photoPosition);
                        photos.removeViewAt(photoPosition);
                        break;
                }
            }

            @Override
            public void onDismiss() {

            }
        });

        ViewGroup content = (ViewGroup) activity.findViewById(android.R.id.content);
        if (content != null) {
            content.addView(actionView);
        }
    }

    /**
     * Display the original photo in the photo viewer.
     *
     * @param photo The photo to be shown.
     */
    public void showPhotoViewer(Media photo) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + photo.uri), photo.type);

        activity.startActivity(intent);
    }

    /**
     * Display the photo picker to add photos to the event.
     */
    public void showPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);

        activity.startActivityForResult(Intent.createChooser(intent, "Select Photos"),
            EventDetailsActivity.REQUEST_PHOTO_PICKER);
    }

    /**
     * Handle the result from the photo picker. The result can either be a single or a set of
     * photos returned from the photo picker.
     *
     * @param resultCode The result code returned from the activity.
     * @param data The data returned from the activity.
     */
    public void handlePhotoPicker(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (data.getClipData() == null) {
                // Handle Single Image
                addPhoto(data.getData());
            } else {
                // Handle Multiple Images
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    addPhoto(clipData.getItemAt(i).getUri());
                }
            }
        }
    }

    /**
     * Photos retrieved from the photo picker will be added to the event.
     *
     * @param contentUri The content URI of the photo.
     */
    private void addPhoto(Uri contentUri) {
        Uri uri = UriHelper.resolve(activity, contentUri);
        // Check File Exists
        String path = uri.toString();
        if (!Common.fileExists(path)) {
            return;
        }
        // Check File Size
        long size = Common.fileSize(path);
        // Get Photo
        Media photo = MediaManager.getInstance(activity).getImage(path, size);
        // Prevent Duplicate Photo
        if (event.photos.contains(photo)) {
            return;
        }
        // Add Photo to Event
        event.photos.add(photo);
        createPhoto(uri, photo.thumbnail);
    }
}
