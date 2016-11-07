package com.mono.dashboard;

import android.graphics.Bitmap;

import com.mono.model.Media;

import java.util.ArrayList;
import java.util.List;

/**
 * This data structure is used to store information about a specific photo event item.
 *
 * @author Gary Ng
 */
public class PhotoEventItem extends EventItem {

    public List<Media> photos;
    public List<Bitmap> bitmaps = new ArrayList<>();

    public PhotoEventItem(String id) {
        super(id);
    }
}
