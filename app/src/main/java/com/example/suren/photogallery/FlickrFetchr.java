package com.example.suren.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {
    private static final String LOG_TAG = FlickrFetchr.class.getSimpleName();
    private static final String FLICKR_ENTRY_POINT =
            "https://api.flickr.com/services/rest/";
    private static final String FLICKR_KEY = "<get API key at http://www.flickr.com/services/api/>";

    private static void parseJson(List<GalleryItem> items, JSONObject root) throws JSONException {
        JSONObject photos = root.getJSONObject("photos");
        JSONArray photoArray = photos.getJSONArray("photo");
        for (int i = 0; i < photoArray.length(); i++) {
            JSONObject photo = photoArray.getJSONObject(i);
            if (photo.has("url_s")) {
                GalleryItem item = new GalleryItem();
                item.setId(photo.getString("id"));
                item.setCaption(photo.getString("title"));
                item.setUrl(photo.getString("url_s"));
                items.add(item);
            }
        }
    }

    public static List<GalleryItem> fetchItems(int page) {
        String urlString = Uri.parse(FLICKR_ENTRY_POINT)
                .buildUpon()
                .appendQueryParameter("method", "flickr.photos.getRecent")
                .appendQueryParameter("api_key", FLICKR_KEY)
                .appendQueryParameter("format", "json")
                .appendQueryParameter("nojsoncallback", "1")
                .appendQueryParameter("extras", "url_s")
                .appendQueryParameter("page", Integer.toString(page))
                .build().toString();
        try {
            String jsonString = getUrlString(urlString);
            List<GalleryItem> items = new ArrayList<GalleryItem>();
            parseJson(items, new JSONObject(jsonString));
            return items;
        } catch (IOException ex) {
            Log.i(LOG_TAG, "Failed to fetch content", ex);
        } catch (JSONException ex) {
            Log.i(LOG_TAG, "Failed to parse content", ex);
        }
        return null;
    }

    public static List<GalleryItem> searchItems(int page, String query) {
        String urlString = Uri.parse(FLICKR_ENTRY_POINT)
                .buildUpon()
                .appendQueryParameter("method", "flickr.photos.search")
                .appendQueryParameter("api_key", FLICKR_KEY)
                .appendQueryParameter("format", "json")
                .appendQueryParameter("nojsoncallback", "1")
                .appendQueryParameter("extras", "url_s")
                .appendQueryParameter("page", Integer.toString(page))
                .appendQueryParameter("text", query)
                .build().toString();
        try {
            String jsonString = getUrlString(urlString);
            List<GalleryItem> items = new ArrayList<GalleryItem>();
            parseJson(items, new JSONObject(jsonString));
            return items;
        } catch (IOException ex) {
            Log.i(LOG_TAG, "Failed to fetch content", ex);
        } catch (JSONException ex) {
            Log.i(LOG_TAG, "Failed to parse content", ex);
        }
        return null;
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
