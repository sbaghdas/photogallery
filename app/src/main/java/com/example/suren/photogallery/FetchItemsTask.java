package com.example.suren.photogallery;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
    private static final String LOG_TAG = FetchItemsTask.class.getSimpleName();
    private static final String SITE_URL = "https://www.bignerdranch.com";

    public interface Listener {
        void onListFetched(List<GalleryItem> list);
    }

    private Listener mListener;

    public FetchItemsTask(Listener listener) {
        super();
        mListener = listener;
    }

    @Override
    protected List<GalleryItem> doInBackground(Void... voids) {
        return FlickrFetchr.fetchItems();
    }

    @Override
    protected void onPostExecute(List<GalleryItem> galleryItems) {
        if (mListener != null) {
            mListener.onListFetched(galleryItems);
        }
    }

}
