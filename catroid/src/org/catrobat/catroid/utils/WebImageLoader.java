/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2016 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import org.catrobat.catroid.R;

public class WebImageLoader {

    private static final String TAG = WebImageLoader.class.getSimpleName();
    public static final int PLACEHOLDER_IMAGE_RESOURCE = R.drawable.ic_launcher;

    private MemCache memoryCache = new MemCache();
    private FileCache fileCache;
    private Map<ImageView, String> imageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    private ExecutorService executorService;

    public WebImageLoader(FileCache fileCache, ExecutorService executorService) {
        this.fileCache = fileCache;
        this.executorService = executorService;
    }

    private class PhotoToLoad {
        public String url;
        public ImageView imageView;

        public PhotoToLoad(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }
    }

    // used to display bitmap (executed by UI thread)
    private class BitmapDisplayer implements Runnable {
        public Bitmap bitmap;
        public PhotoToLoad photoToLoad;

        public BitmapDisplayer(Bitmap bitmap, PhotoToLoad photoToLoad){
            this.bitmap = bitmap;
            this.photoToLoad = photoToLoad;
        }

        public void run() {
            if (! Looper.getMainLooper().equals(Looper.myLooper())) {
                throw new AssertionError("You should not change the UI from any thread "
                        + "except UI thread!");
            }

            if (imageViewReused(photoToLoad)) {
                Log.d(TAG, "REUSED!");
                return;
            }
            if (bitmap != null) {
                Log.d(TAG, "Bitmap given!");
                photoToLoad.imageView.setImageBitmap(bitmap);
            } else {
                Log.d(TAG, "Bitmap NOT given!");
                photoToLoad.imageView.setImageBitmap(null);//.setImageResource(PLACEHOLDER_IMAGE_RESOURCE);
            }
        }
    }

    public void fetchAndShowImage(String url, ImageView imageView) {
        Log.d(TAG, "Fetching image from URL: " + url);
        imageViews.put(imageView, url);
        Bitmap bitmap = memoryCache.get(url);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageBitmap(null);//.setImageResource(PLACEHOLDER_IMAGE_RESOURCE);
            // enqueue photo
            executorService.submit(new PhotosLoader(new PhotoToLoad(url, imageView)));
        }
    }

    // decodes image & scales it down in order to reduce memory consumption
    private Bitmap decodeFile(File file) {
        Log.d(TAG, "Decoding file");
        Bitmap bitmap = null;
        InputStream inputStream = null;
        InputStream inputStreamDecoded = null;
        try {
            // decode image size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            inputStream = new FileInputStream(file);
            BitmapFactory.decodeStream(inputStream, null, options);

            // TODO: use appropriate method from ImageEditing class instead
            // find correct scale value (it should be of power of 2)
            final int REQUIRED_SIZE = 70;
            int widthTemp = options.outWidth;
            int heightTemp = options.outHeight;
            int scale = 1;
            while (true) {
                if (((heightTemp / 2) < REQUIRED_SIZE) || ((widthTemp / 2) < REQUIRED_SIZE)) {
                    break;
                }
                heightTemp /= 2;
                widthTemp /= 2;
                scale *= 2;
            }

            // decode with inSampleSize
            options = new BitmapFactory.Options();
            options.inSampleSize = scale;
            inputStreamDecoded = new FileInputStream(file);
            bitmap = BitmapFactory.decodeStream(inputStreamDecoded, null, options);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "An exception occured: " + e.getLocalizedMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {}
            }
            if (inputStreamDecoded != null) {
                try {
                    inputStreamDecoded.close();
                } catch (IOException e) {}
            }
        }
        return bitmap;
    }

    boolean imageViewReused(PhotoToLoad photoToLoad){
        String tag = imageViews.get(photoToLoad.imageView);
        if (tag == null || (! tag.equals(photoToLoad.url))) {
            return true;
        }
        return false;
    }

    public void clearCache() {
        memoryCache.clear();
        fileCache.clear();
    }

    // Task for the queue
    class PhotosLoader implements Runnable {
        PhotoToLoad photoToLoad;
        PhotosLoader(PhotoToLoad photoToLoad) {
            this.photoToLoad = photoToLoad;
        }

        @Override
        public void run() {
            if (imageViewReused(photoToLoad)) {
                return;
            }
            File file = fileCache.getFile(photoToLoad.url);
            Bitmap bitmap = null;
            if (file.exists()) {
                bitmap = decodeFile(file);
            }
            if (bitmap == null) {
                bitmap = fetchBitmapFromWeb(photoToLoad.url, file);
            }
            memoryCache.put(photoToLoad.url, bitmap);
            if (imageViewReused(photoToLoad)) {
                return;
            }
            BitmapDisplayer bitmapDisplayer = new BitmapDisplayer(bitmap, photoToLoad);
            ((Activity)photoToLoad.imageView.getContext()).runOnUiThread(bitmapDisplayer);
        }

        // TODO: move to server calls later
        private Bitmap fetchBitmapFromWeb(String url, File file) {
            Log.d(TAG, "Downloading image from URL: " + url);
            InputStream inputStream = null;
            OutputStream outputStream = null;
            Bitmap bitmap = null;

            try {
                URL imageUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection)imageUrl.openConnection();
                connection.setConnectTimeout(30000); // TODO: outsource settings!
                connection.setReadTimeout(30000); // TODO: outsource settings!
                connection.setInstanceFollowRedirects(true);
                inputStream = connection.getInputStream();
                outputStream = new FileOutputStream(file);
                Utils.copyStream(inputStream, outputStream);
                bitmap = decodeFile(file);
            } catch (Throwable ex) {
                ex.printStackTrace();
                if (ex instanceof OutOfMemoryError) {
                    memoryCache.clear();
                }
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException exception) {}
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException exception) {}
                }
            }
            return bitmap;
        }

    }

}
