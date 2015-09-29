package com.sismoplatform.sismoapp;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Created by pedro on 24/08/15.
 */
public class HTTPClient {
    HttpURLConnection connection;
    public String url;
    public String method;
    public String headers;

    public HTTPClient(String url) throws Exception {
        URL urlObject = new URL(url);
        this.url = url;
        this.connection = (HttpURLConnection) urlObject.openConnection();
    }

    public void setMethod(String method) throws Exception{
        this.method = method;
        this.connection.setRequestMethod(method);

        if(this.method != "GET"){
            this.connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        }
        this.connection.setRequestProperty("Content-length", "0");
        this.connection.setUseCaches(false);
        this.connection.setAllowUserInteraction(false);
    }

    public void addHeader(String key, String value) throws Exception {
        this.connection.setRequestProperty(key, value);
    }

    public void addParams(String params) throws Exception {
        byte[] data = params.getBytes(StandardCharsets.UTF_8);
        int dataLength = data.length;
        this.connection.setRequestProperty("Content-Length", Integer.toString(dataLength));
        try( DataOutputStream wr = new DataOutputStream(this.connection.getOutputStream())) {
            wr.write( data );
        }
    }

    public String makeRequest() throws Exception{
        this.connection.connect();

        int responseCode = this.connection.getResponseCode();
        System.out.println("\nSending '"+this.method+"' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        InputStream response;
        if(this.isErrorCode(responseCode)){
            response = this.connection.getErrorStream();
        }else{
            response = this.connection.getInputStream();
        }
        return getJSONStringFromRequest(response);
    }

    public boolean isErrorCode(int errorCode) {
        if(errorCode >= 400){
            return true;
        }else{
            return false;
        }
    }

    public String getJSONStringFromRequest(InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}

