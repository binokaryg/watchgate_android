package com.binokary.watchgate;

import android.content.Context;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public final class SlackHelper {
    private static final String TAG = Constants.MAIN_TAG + "SLACK";
    private static String SLACK_HOOK;

    public static void sendMessage(Context appContext, JSONObject jsonBody) {
        SLACK_HOOK = appContext.getResources().getString(R.string.slack_url);
        //Push to Slack Channel
        RequestQueue requestQueue = Volley.newRequestQueue(appContext);
        final String requestBody = jsonBody.toString();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, SLACK_HOOK,
                response -> Log.i(TAG + " VOLLEY", response), error -> Log.e(TAG + " VOLLEY", error.toString())) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                return requestBody.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {

                assert response != null;
                return Response.success(String.valueOf(response.statusCode), HttpHeaderParser.parseCacheHeaders(response));
            }
        };

        requestQueue.add(stringRequest);
    }
}