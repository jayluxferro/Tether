package com.koushikdutta.tether;

import android.net.http.AndroidHttpClient;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONException;
import org.json.JSONObject;

class StreamUtility {

    StreamUtility() {

    }

    public static int copyStream(InputStream input, OutputStream output, Callback<Integer, Boolean> callback) throws IOException {
        byte[] stuff = new byte[65536];
        int total = 0;
        while (true) {
            int read = input.read(stuff);
            if (read == -1) {
                return total;
            }
            output.write(stuff, 0, read);
            total += read;
            if (callback != null && !callback.onCallback(total)) {
                return total;
            }
        }
    }

    public static int copyStream(InputStream input, OutputStream output) throws IOException {
        return copyStream(input, output, null);
    }

    public static String downloadUriAsString(String uri) throws IOException {
        return downloadUriAsString(new HttpGet(uri));
    }

    public static void downloadUri(String uri, String filename, final Callback<Float, Boolean> callback) throws IOException {
        HttpGet get = new HttpGet(uri);
        File file = new File(filename);
        file.getParentFile().mkdirs();
        FileOutputStream fout = new FileOutputStream(file);
        AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        try {
            HttpResponse res = client.execute(get);
            final float length = (float) res.getEntity().getContentLength();
            copyStream(res.getEntity().getContent(), fout, new Callback<Integer, Boolean>() {
                public Boolean onCallback(Integer result) {
                    return callback.onCallback((((float) result) / length) * 100.0f);
                }
            });
        } finally {
            client.close();
            fout.close();
        }
    }

    public static String downloadUriAsString(HttpUriRequest req) throws IOException {
        AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        try {
            return downloadUriAsString(client.execute(req));
        } finally {
            client.close();
        }
    }

    public static JSONObject downloadUriAsJSONObject(String uri) throws IOException, JSONException {
        return new JSONObject(downloadUriAsString(uri));
    }

    public static JSONObject downloadUriAsJSONObject(HttpResponse res) throws IOException, JSONException {
        return new JSONObject(downloadUriAsString(res));
    }

    public static JSONObject downloadUriAsJSONObject(HttpUriRequest req) throws IOException, JSONException {
        return new JSONObject(downloadUriAsString(req));
    }

    public static String downloadUriAsString(HttpResponse res) throws IllegalStateException, IOException {
        return readToEnd(res.getEntity().getContent());
    }

    public static byte[] readToEndAsArray(InputStream input) throws IOException {
        DataInputStream dis = new DataInputStream(input);
        byte[] stuff = new byte[1024];
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        while (true) {
            int read = dis.read(stuff);
            if (read == -1) {
                return buff.toByteArray();
            }
            buff.write(stuff, 0, read);
        }
    }

    public static String readToEnd(InputStream input) throws IOException {
        return new String(readToEndAsArray(input));
    }

    public static String readFile(String filename) throws IOException {
        return readFile(new File(filename));
    }

    public static String readFile(File file) throws IOException {
        byte[] buffer = new byte[((int) file.length())];
        new DataInputStream(new FileInputStream(file)).readFully(buffer);
        return new String(buffer);
    }

    public static void writeFile(File file, String string) throws IOException {
        writeFile(file.getAbsolutePath(), string);
    }

    public static void writeFile(String file, String string) throws IOException {
        File f = new File(file);
        f.getParentFile().mkdirs();
        DataOutputStream dout = new DataOutputStream(new FileOutputStream(f));
        dout.write(string.getBytes());
        dout.close();
    }
}