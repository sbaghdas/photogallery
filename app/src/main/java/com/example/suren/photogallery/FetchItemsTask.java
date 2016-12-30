package com.example.suren.photogallery;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
    private static final String LOG_TAG = FetchItemsTask.class.getSimpleName();
    private static final String SITE_URL = "https://www.bignerdranch.com";

    public interface Listener {
        void onListFetched(List<GalleryItem> list, int page);
    }

    private Listener mListener;
    private int mPage;

    public FetchItemsTask(Listener listener, int page) {
        super();
        mListener = listener;
        mPage = page;
    }

    @Override
    protected List<GalleryItem> doInBackground(Void... voids) {
        return FlickrFetchr.fetchItems(mPage);
    }

    @Override
    protected void onPostExecute(List<GalleryItem> galleryItems) {
        if (mListener != null) {
            mListener.onListFetched(galleryItems, mPage);
        }
    }

}
