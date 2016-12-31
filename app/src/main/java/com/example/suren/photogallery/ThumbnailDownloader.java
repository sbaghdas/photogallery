package com.example.suren.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final String LOG_TAG = ThumbnailDownloader.class.getSimpleName();
    private static final int MESSAGE_DOWNLOAD = 0;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private Handler mUiHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<T, String>();
    private ThumbnailDownloaderListener<T> mListener;

    public ThumbnailDownloader(Handler uiHandler) {
        super(TAG);
        mUiHandler = uiHandler;
    }

    public void queueThumbnail(T target, String url) {
        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
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
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T)msg.obj;
                    handleRequest(target);
                } else {
                    super.handleMessage(msg);
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

    private void handleRequest(T target) {
        String urlString = mRequestMap.get(target);
        if (urlString != null) {
            try {
                byte[] bytes = FlickrFetchr.getUrlBytes(urlString);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                mUiHandler.post(new UiUpdateTask(target, urlString, bitmap));
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Failed to fetch image", ex);
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
