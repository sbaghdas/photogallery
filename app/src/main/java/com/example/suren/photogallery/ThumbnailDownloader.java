package com.example.suren.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.util.ArraySet;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final String LOG_TAG = ThumbnailDownloader.class.getSimpleName();
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MESSAGE_PRELOAD = 1;
    private static final int CACHE_SIZE = 50;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private Handler mUiHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<T, String>();
    private ThumbnailDownloaderListener<T> mListener;
    private LruCache<String, Bitmap> mBitmapCache = new LruCache<String, Bitmap>(CACHE_SIZE);
    private Set<String> mPreloadRequestSet = new ArraySet<>();
    private final Object mPreloadLock = new Object();

    public ThumbnailDownloader(Handler uiHandler) {
        super(TAG);
        mUiHandler = uiHandler;
    }

    public void queueThumbnail(T target, String url) {
        if (url == null) {
            mRequestMap.remove(target);
        } else {
            if (target != null) {
                mRequestMap.put(target, url);
                mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                        .sendToTarget();
                // remove all pending preload requests
                mRequestHandler.removeMessages(MESSAGE_PRELOAD);
            } else {
                synchronized(mPreloadLock) {
                    mPreloadRequestSet.add(url);
                }
                if (!mRequestHandler.hasMessages(MESSAGE_DOWNLOAD)) {
                    mRequestHandler.obtainMessage(MESSAGE_PRELOAD)
                            .sendToTarget();
                }
            }
        }
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        // create Handler here because it is called on the new Looper's thread and
        // is called before looper checks for it's message queue
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                T target = null;
                switch (msg.what) {
                    case (MESSAGE_DOWNLOAD):
                        target = (T)msg.obj;
                        // fall through
                    case (MESSAGE_PRELOAD):
                        handleRequest(target);
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        };
    }

    public interface ThumbnailDownloaderListener<T> {
        void onThumbnailDownload(T target, Bitmap bitmap);
    }

    public void setThumbnailDownloaderListener(ThumbnailDownloaderListener<T> listener) {
        mListener = listener;
    }

    private Bitmap downloadBitmap(String urlString) {
        Bitmap bitmap = mBitmapCache.get(urlString);
        if (bitmap == null) {
            try {
                byte[] bytes = FlickrFetchr.getUrlBytes(urlString);
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                mBitmapCache.put(urlString, bitmap);
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Failed to fetch image", ex);
            }
        } else {
            Log.i(LOG_TAG, "Cache hit on " + urlString);
        }
        return bitmap;
    }

    private void handleRequest(T target) {
        if (target != null) {
            String urlString = mRequestMap.get(target);
            if (urlString != null) {
                synchronized(mPreloadLock) {
                    mPreloadRequestSet.remove(urlString);
                }
                Log.i(LOG_TAG, "Downloading " + urlString);
                Bitmap bitmap = downloadBitmap(urlString);
                if (bitmap != null) {
                    mUiHandler.post(new UiUpdateTask(target, urlString, bitmap));
                }
            }
        } else {
            String urlString = null;
            synchronized(mPreloadLock) {
                Iterator<String> iter = mPreloadRequestSet.iterator();
                if (iter.hasNext()) {
                    urlString = iter.next();
                    iter.remove();
                } else {
                    mRequestHandler.removeMessages(MESSAGE_PRELOAD);
                }
            }
            if (urlString != null) {
                Log.i(LOG_TAG, "Preloading " + urlString);
                downloadBitmap(urlString);
            }
        }
        if (!mRequestHandler.hasMessages(MESSAGE_DOWNLOAD)) {
            boolean morePreload;
            synchronized(mPreloadLock) {
                morePreload = !mPreloadRequestSet.isEmpty();
            }
            if (morePreload) {
                mRequestHandler.obtainMessage(MESSAGE_PRELOAD, target)
                        .sendToTarget();
            }
        }
    }

    private class UiUpdateTask implements Runnable {
        private T mTarget;
        private String mUrl;
        private Bitmap mBitmap;

        public UiUpdateTask(T target, String url, Bitmap bitmap) {
            mTarget = target;
            mUrl = url;
            mBitmap = bitmap;
        }

        @Override
        public void run() {
            if (!mHasQuit && mRequestMap.get(mTarget) == mUrl) {
                mRequestMap.remove(mTarget);
                mListener.onThumbnailDownload(mTarget, mBitmap);
            }
        }
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }
}
