package com.example.tnpxu.opencvbook.api;

import com.squareup.okhttp.RequestBody;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Query;
import retrofit.Call;

/**
 * Created by tnpxu on 11/12/2558.
 */
public interface SendingPhotoApi {
    @Multipart
    @POST("/api/upload")
    Call<String> upload(
            @Part("drone1\"; filename=\"drone1.jpg\" ") RequestBody file,
            @Part("description") String description);
}
