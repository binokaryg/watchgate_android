package com.binokary.watchgate;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SlackWorker extends Worker {
    private static final String TAG = "SlackWorker";

    public SlackWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String jsonString = getInputData().getString("json_body");
        if (jsonString == null) return Result.failure();

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        try {
            // 1. Get the URL from resources (Context is available via getApplicationContext())
            String hookUrl = getApplicationContext().getString(R.string.slack_url);

            StringRequest stringRequest = new StringRequest(Request.Method.POST, hookUrl,
                    response -> {
                        Log.i(TAG, "Slack Success: " + response);
                        success[0] = true;
                        latch.countDown(); // Release the lock
                    },
                    error -> {
                        Log.e(TAG, "Slack Error: " + error.toString());
                        latch.countDown(); // Release the lock
                    }
            ) {
                // 2. CRITICAL: You must override these to send the JSON body
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() {
                    return jsonString.getBytes(StandardCharsets.UTF_8);
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    // PROBLEM FIXED: Value 'response' is always 'null' check.
                    // If response is null, super.parseNetworkResponse(response) would crash.
                    if (response == null || response.data == null) {
                        return Response.error(new VolleyError("Empty response"));
                    }
                    return super.parseNetworkResponse(response);
                }
            };

            // 3. Add to queue
            Volley.newRequestQueue(getApplicationContext()).add(stringRequest);

            // 4. Wait for Volley to finish (Max 30 seconds)
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                Log.e(TAG, "Slack request timed out");
                return Result.retry();
            }

        } catch (Exception e) {
            Log.e(TAG, "Worker Exception: " + e.getMessage());
            return Result.failure();
        }

        return success[0] ? Result.success() : Result.retry();
    }
}