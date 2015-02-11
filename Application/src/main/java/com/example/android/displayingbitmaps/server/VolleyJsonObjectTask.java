package com.example.android.displayingbitmaps.server;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.android.displayingbitmaps.ui.PhotoSearchApplication;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nitesh on 26/1/15.
 */
public class VolleyJsonObjectTask extends AsyncTask<Void, Void, Void> {

    private Context mContext;
    private String mUrl = "";
    private Callback mCallback;
    private JSONObject mParams;

    public VolleyJsonObjectTask(Context context, String url, JSONObject params, Callback callBack) {
        this.mContext = context;
        this.mUrl = url;
        this.mParams = params;
        this.mCallback = callBack;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... params) {

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST, mUrl, mParams,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        mCallback.callSuccess(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d("error", "Error: " + error.getMessage());
                mCallback.callFailed(error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        try {
            Map<String, String> map = jsonObjReq.getHeaders();
            for(String key: map.keySet()) {
                System.out.println(key);
                System.out.println(map.get(key));
            }
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
        }

        PhotoSearchApplication.getInstance().addToRequestQueue(jsonObjReq);

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }

    /**
     * Class to send the response back to calling class
     */
    public static class Callback {
        public void callSuccess(JSONObject result) {

        }

        public boolean callFailed(Exception e) {
            return true;
        }
    }
}
