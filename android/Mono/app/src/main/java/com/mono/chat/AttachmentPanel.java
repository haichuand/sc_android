package com.mono.chat;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.mono.MainActivity;
import com.mono.MediaManager;
import com.mono.R;
import com.mono.model.Media;
import com.mono.model.Message;
import com.mono.network.HttpServerManager;
import com.mono.network.NetworkManager;
import com.mono.util.BitmapHelper;
import com.mono.util.Colors;
import com.mono.util.Common;
import com.mono.util.Pixels;
import com.mono.util.SimpleQuickAction;
import com.mono.util.UriHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This class is used to handle the attachments section located in Chat Room.
 *
 * @author Gary Ng
 */
public class AttachmentPanel {

    public static final int CAMERA_TYPE_IMAGE = 0;
    public static final int CAMERA_TYPE_VIDEO = 1;

    private static final int IMAGE_WIDTH_DP = 120;
    private static final int IMAGE_HEIGHT_DP = 90;
    private static final int ITEM_LIMIT = 1;
    private static final long SIZE_LIMIT = 50000000L;
    private static final int MAX_DIMENSION = 1536;

    private static final String[] MEDIA_ACTIONS = {"View", "Remove"};
    private static final int MEDIA_ACTION_VIEW = 0;
    private static final int MEDIA_ACTION_REMOVE = 1;

    private static final SimpleDateFormat DATETIME_FORMAT;

    private ChatRoomActivity activity;
    private ViewGroup container;
    private ViewGroup itemsView;

    private List<Media> attachments = new ArrayList<>();

    private Uri cameraOutputURI;

    static {
        DATETIME_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    }

    public AttachmentPanel(ChatRoomActivity activity) {
        this.activity = activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        container = (ViewGroup) activity.findViewById(R.id.attachment_layout);
        itemsView = (ViewGroup) container.findViewById(R.id.attachment_items);

        ImageButton attachmentButton = (ImageButton) activity.findViewById(R.id.attachment_btn);
        attachmentButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onAttachmentClick();
            }
        });

        ImageButton mediaButton = (ImageButton) activity.findViewById(R.id.media_btn);
        mediaButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showMediaPicker();
            }
        });

        ImageButton cameraButton = (ImageButton) activity.findViewById(R.id.camera_btn);
        cameraButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCameraClick();
            }
        });
    }

    /**
     * Handle attachment button click by setting the visibility of the attachments section.
     */
    public void onAttachmentClick() {
        if (container.getVisibility() == View.VISIBLE) {
            clear();
        } else {
            container.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Create a thumbnail from a byte array otherwise attempt to load it using the path given.
     * The resulting thumbnail will be appended to the attachments section.
     *
     * @param uri The image path.
     * @param data The image data.
     */
    public void createImage(Uri uri, byte[] data, int overlayResId) {
        int width = Pixels.pxFromDp(activity, IMAGE_WIDTH_DP);
        int height = Pixels.pxFromDp(activity, IMAGE_HEIGHT_DP);

        View view = BitmapHelper.createThumbnail(activity, uri, data, overlayResId, width, height);
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onMediaClick(itemsView.indexOfChild(view));
            }
        });

        itemsView.addView(view);
        itemsView.setVisibility(View.VISIBLE);
    }

    /**
     * Handle the action of clicking on the media. A popup of additional options will be shown
     * upon click.
     *
     * @param position The position of the media.
     */
    public void onMediaClick(int position) {
        final int itemPosition = position;

        View view = itemsView.getChildAt(position);

        SimpleQuickAction actionView = SimpleQuickAction.newInstance(activity);
        actionView.setColor(Colors.getColor(activity, R.color.colorPrimary));
        actionView.setActions(MEDIA_ACTIONS);

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
                    case MEDIA_ACTION_VIEW:
                        Media attachment = attachments.get(itemPosition);
                        showMediaViewer(attachment);
                        break;
                    case MEDIA_ACTION_REMOVE:
                        attachments.remove(itemPosition);
                        itemsView.removeViewAt(itemPosition);

                        if (itemsView.getChildCount() == 0) {
                            itemsView.setVisibility(View.GONE);
                        }
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
     * Display the original media such as image or video in the media viewer.
     *
     * @param media The media to be shown.
     */
    public void showMediaViewer(Media media) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + media.uri), media.type);

        activity.startActivity(intent);
    }

    /**
     * Display the media picker to add media as attachments to the chat.
     */
    public void showMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.setAction(Intent.ACTION_GET_CONTENT);

        activity.startActivityForResult(intent, ChatRoomActivity.REQUEST_MEDIA_PICKER);
    }

    /**
     * Handle the result from the media picker. The result can either be a single or a set of
     * media returned from the media picker.
     *
     * @param resultCode The result code returned from the activity.
     * @param data The data returned from the activity.
     */
    public void handleMediaPicker(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (data.getClipData() == null) {
                // Handle Single Media
                addMedia(data.getData());
            } else {
                // Handle Multiple Media
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    addMedia(clipData.getItemAt(i).getUri());
                }
            }
        }
    }

    /**
     * Media retrieved from the media picker will be added to the chat message.
     *
     * @param contentUri The content URI of the media.
     */
    private void addMedia(Uri contentUri) {
        Uri uri = UriHelper.resolve(activity, contentUri);
        // Check File Exists
        String path = uri.toString();
        if (!Common.fileExists(path)) {
            return;
        }
        // Check File Size
        long size = Common.fileSize(path);
        if (size > SIZE_LIMIT) {
            String text = activity.getString(R.string.media_size_limit, SIZE_LIMIT / 1000000 + " MB");
            Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
            return;
        }
        // Get MIME Type
        String mimeType = activity.getContentResolver().getType(contentUri);
        if (mimeType == null) {
            return;
        }

        byte[] thumbnail = null;
        int overlayResId = 0;

        if (mimeType.startsWith("image/")) {
            mimeType = Media.IMAGE;
            thumbnail = MediaManager.createThumbnail(path);
        } else if (mimeType.startsWith("video/")) {
            mimeType = Media.VIDEO;
            thumbnail = MediaManager.createVideoThumbnail(path);
            overlayResId = R.drawable.ic_play_circle_outline;
        }

        Media media = new Media(Uri.parse(path), mimeType, size);
        media.thumbnail = thumbnail;
        // Prevent Duplicate Media
        if (attachments.contains(media)) {
            return;
        }
        // Append Media
        attachments.add(media);
        createImage(uri, thumbnail, overlayResId);
    }

    /**
     * Handle camera button click and display camera view.
     */
    public void onCameraClick() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            try {
                cameraOutputURI = createCameraFile(CAMERA_TYPE_IMAGE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutputURI);
                activity.startActivityForResult(intent, ChatRoomActivity.REQUEST_CAMERA);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Create temporary file for the camera to use.
     *
     * @param type Type of camera mode.
     * @return Path of temporary file.
     * @throws IOException
     */
    private Uri createCameraFile(int type) throws IOException {
        String dirType, extension;

        if (type == CAMERA_TYPE_VIDEO) {
            dirType = Environment.DIRECTORY_MOVIES;
            extension = ".mp4";
        } else {
            dirType = Environment.DIRECTORY_PICTURES;
            extension = ".jpg";
        }

        File directory = Environment.getExternalStoragePublicDirectory(dirType);
        String filename = String.format("%s_%s",
            activity.getString(R.string.app_name),
            DATETIME_FORMAT.format(new Date())
        );

        String path = directory + File.separator + filename + extension;
        return Uri.fromFile(new File(path));
    }

    /**
     * Handle the result from the camera. Add media to device gallery.
     *
     * @param resultCode The result code returned from the activity.
     * @param data The data returned from the activity.
     */
    public void handleCamera(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String path = cameraOutputURI.getPath();

            MediaScannerConnection.scanFile(activity, new String[]{path}, null,
                new MediaScannerConnection.MediaScannerConnectionClient() {
                    @Override
                    public void onMediaScannerConnected() {

                    }

                    @Override
                    public void onScanCompleted(String path, final Uri uri) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addMedia(uri);
                            }
                        });
                    }
                }
            );
        }
    }

    /**
     * Retrieve list of existing attachments.
     *
     * @return a list of attachments.
     */
    public List<Media> getAttachments() {
        return attachments;
    }

    /**
     * Upload media to the the server.
     *
     * @param message Reference to message object.
     * @param listener Callback for upload progress.
     */
    public void sendAttachments(final Message message, final AttachmentsListener listener) {
        final int[] counter = {0};
        final int size = message.attachments.size();
        final List<String> result = new ArrayList<>();

        NetworkManager manager = NetworkManager.getInstance(activity);

        for (Media media : message.attachments) {
            String url = null;
            String path = media.uri.toString();

            switch (media.type) {
                case Media.IMAGE:
                    url = HttpServerManager.UPLOAD_IMAGE_URL;

                    try {
                        Bitmap bitmap = BitmapHelper.createBitmap(path, MAX_DIMENSION, MAX_DIMENSION);

                        String storage = Environment.getExternalStorageDirectory().getPath() + "/";
                        String filename = String.format("%s_temp", activity.getString(R.string.app_name));
                        path = storage + MainActivity.APP_DIR + filename + ".jpg";

                        OutputStream outputStream = new FileOutputStream(path);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);

                        outputStream.flush();
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case Media.VIDEO:
                    url = HttpServerManager.UPLOAD_VIDEO_URL;
                    break;
            }

            if (url == null) {
                continue;
            }

            manager.post(url, path, new NetworkManager.ResponseListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }

                @Override
                public void onResponse(JSONObject response) {
                    counter[0]++;

                    try {
                        String status = response.getString("status");

                        if (status.equalsIgnoreCase("OK")) {
                            String filename = response.getString("filename");
                            String type = response.getString("type");
                            result.add(type + ":" + filename);
                        } else {
                            String msg = response.getString("msg");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (counter[0] == size) {
                        listener.onFinish(message, result);
                    }
                }
            });
        }
    }

    /**
     * Remove existing attachments and hide view.
     */
    public void clear() {
        container.setVisibility(View.GONE);
        itemsView.setVisibility(View.GONE);

        itemsView.removeAllViews();
        attachments = new ArrayList<>();
    }

    public interface AttachmentsListener {

        void onFinish(Message message, List<String> result);
    }
}
