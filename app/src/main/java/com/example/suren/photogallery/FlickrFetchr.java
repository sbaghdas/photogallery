package com.example.suren.photogallery;

import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FlickrFetchr {
    private static final String LOG_TAG = FlickrFetchr.class.getSimpleName();
    private static final String FLICKR_ENTRY_POINT =
            "https://api.flickr.com/services/rest/";
    private static final String FLICKR_KEY = "<get API key at http://www.flickr.com/services/api/>";

    public static void fetchItems() {
        String urlString = Uri.parse(FLICKR_ENTRY_POINT)
                .buildUpon()
                .appendQueryParameter("method", "flickr.photos.getRecent")
                .appendQueryParameter("api_key", FLICKR_KEY)
                .appendQueryParameter("format", "json")
                .appendQueryParameter("nojsoncallback", "1")
                .appendQueryParameter("extras", "url_s")
                .build().toString();
        try {
            String jsonString = getUrlString(urlString);
            Log.i(LOG_TAG, jsonString);
        } catch (IOException ex) {
            Log.i(LOG_TAG, "Failed to fetch content", ex);
        }
    }

    public static byte[] getUrlBytes(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            InputStream in = connection.getInputStream();
            int bytesRead;
            byte[] bytes = new byte[1024];
            while ((bytesRead = in.read(bytes)) > 0) {
                out.write(bytes, 0, bytesRead);
            }
            out.close();
        } finally {
            connection.disconnect();
        }
        return out.toByteArray();
    }

    public static String getUrlString(String urlString) throws IOException {
        return new String(getUrlBytes(urlString));
    }
}
