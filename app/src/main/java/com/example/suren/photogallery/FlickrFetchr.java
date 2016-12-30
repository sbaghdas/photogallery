package com.example.suren.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FlickrFetchr {
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
