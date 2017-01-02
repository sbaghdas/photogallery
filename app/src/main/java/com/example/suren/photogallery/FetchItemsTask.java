package com.example.suren.photogallery;

import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
    private static final String LOG_TAG = FetchItemsTask.class.getSimpleName();

    public interface Listener {
        void onListFetched(List<GalleryItem> list, int page);
    }

    private Listener mListener;
    private final int mPage;
    private final String mQuery;

    public FetchItemsTask(Listener listener, String query, int page) {
        super();
        mListener = listener;
        mPage = page;
        mQuery = query;
    }

    @Override
    protected List<GalleryItem> doInBackground(Void... voids) {
        if (mQuery == null) {
            return FlickrFetchr.fetchItems(mPage);
        } else {
            return FlickrFetchr.searchItems(mPage, mQuery);
        }
    }

    @Override
    protected void onPostExecute(List<GalleryItem> galleryItems) {
        if (mListener != null) {
            mListener.onListFetched(galleryItems, mPage);
        }
    }

}
