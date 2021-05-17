package com.raketlabs.daon.migrate;

import java.util.List;

import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface MigrateService {

    @GET("idchecks")
    Call<ResponseBody> get();

    
    @GET("idchecks")
    Call<List<IdCheck>> getIdChecks();

    
    @GET
    Call<JsonObject> getJsonObject(@Url String url);
    
    
    @GET("idchecks")
    Call<JsonObject> getJsonIdChecks();
    
    
    @GET("users/*/idchecks/{idchecks}/documents")
    Call<JsonObject> getJsonDocuments(@Path("idchecks") String idchecks);
    
    
    @GET("idchecks/{id}/documents/{docId}/serverProcessed/ocrData/sensitiveData")
    Call<JsonObject> getJsonOcrData(@Path("id") String id, @Path("docId") String docId);
    
    
    @GET("idchecks/{id}/documents/{docId}/clientCapture/unprocessedImage/sensitiveData")
    Call<JsonObject> getJsonUnprocessImage(@Path("id") String id, @Path("docId") String docId);
}