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

import java.util.Collections;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import android.graphics.Bitmap;
import android.util.Log;

public class MemCache {

    private static final String TAG = MemCache.class.getSimpleName();
    private Map<String, Bitmap> cache = Collections.synchronizedMap(
            new LinkedHashMap<String, Bitmap>(10, 1.5f, true)); // "true" enables LRU ordering
    private long allocatedSize = 0; // in bytes
    private long maxMemoryLimit = 1000000; // in bytes

    public MemCache() {
        setLimit(Runtime.getRuntime().maxMemory() / 4); // use 25% of available heap size
    }

    public void setLimit(long limit){
        maxMemoryLimit = limit;
        Log.i(TAG, "MemoryCache will use up to " + maxMemoryLimit/1024./1024. + "MB!");
    }

    public Bitmap get(String id){
        try {
            if (! cache.containsKey(id)) {
                return null;
            }
            // NullPointerException sometimes happen here, see: http://code.google.com/p/osmdroid/issues/detail?id=78
            return cache.get(id);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void put(String id, Bitmap bitmap){
        try {
            if (cache.containsKey(id)) {
                allocatedSize -= getSizeInBytes(cache.get(id));
            }
            cache.put(id, bitmap);
            allocatedSize += getSizeInBytes(bitmap);
            checkSize();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private void checkSize() {
        Log.i(TAG, "allocated cache size: " + allocatedSize + ", number of items: " + cache.size());
        if (allocatedSize > maxMemoryLimit) {
            // first iterated item is least recently accessed item
            Iterator<Entry<String, Bitmap>> iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, Bitmap> entry = iterator.next();
                allocatedSize -= getSizeInBytes(entry.getValue());
                iterator.remove();
                if (allocatedSize <= maxMemoryLimit) {
                    break;
                }
            }
            Log.i(TAG, "Cleaned cache, number of items: " + cache.size());
        }
    }

    public void clear() {
        try {
            // NullPointerException sometimes happen here, see: http://code.google.com/p/osmdroid/issues/detail?id=78
            cache.clear();
            allocatedSize = 0;
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    long getSizeInBytes(Bitmap bitmap) {
        if (bitmap == null) {
            return 0;
        }
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

}
