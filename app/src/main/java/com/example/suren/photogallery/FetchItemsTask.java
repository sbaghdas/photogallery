package com.example.suren.photogallery;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

public class FetchItemsTask extends AsyncTask<Void, Void, Void> {
    private static final String LOG_TAG = FetchItemsTask.class.getSimpleName();
    private static final String SITE_URL = "https://www.bignerdranch.com";


    @Override
    protected Void doInBackground(Void... voids) {
        FlickrFetchr.fetchItems();
        return null;
    }
}
