package com.raketlabs.daon.migrate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.Base64;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class App {
    
    public static void main(String[] args) throws IOException {
       
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new BasicAuthInterceptor(Config.BASIC_AUTH_USER, Config.BASIC_AUTH_PASS))
                .build();
        

        Retrofit retrofit = new Retrofit.Builder().baseUrl(Config.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        MigrateService service = retrofit.create(MigrateService.class);
        
        
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new FileReader(Config.CONFIG_FILE));
        JsonObject jsonConfig = gson.fromJson(reader, JsonObject.class);
        
        Optional<JsonElement> root = Optional.ofNullable(jsonConfig.get("root"));
        
        root.ifPresentOrElse(
            r -> crawl(service, r.getAsString()), 
            () -> System.out.println("Can't load config")
        );
        
        System.out.println("end");
    }
    
    
    public static <T> void crawl (MigrateService service, String url) {
        
        System.out.println("Starting to crawl: " + url + "\n");
        
        Call<JsonObject> callIdChecks = service.getJsonObject(url);
        JsonObject jsonIdChecks = execute(callIdChecks);
        
        JsonArray idChecksItems  = jsonIdChecks.getAsJsonArray("items");
        
        
        for (int i=0; i<idChecksItems.size(); i++) {
            
            JsonObject idCheck = (JsonObject) idChecksItems.get(i);
            
            String id = idCheck.get("id").getAsString();
            String href = idCheck.get("href").getAsString();
            
            
            Call<JsonObject> callDocuments = service.getJsonObject(href + "/documents");
            JsonObject jsonDocuments = execute(callDocuments);
            
            if (jsonDocuments != null) {
                
                writeText("data/" + id + ".txt", jsonDocuments.toString());
            } else {
                
                continue;
            }

            
            JsonArray documentItems = jsonDocuments.getAsJsonArray("items");
            
            
            for (int j=0; j<documentItems.size(); j++) {
                
                JsonObject document = (JsonObject) documentItems.get(j);
                
                String documentHref = document.get("href").getAsString();
                
                Call<JsonObject> callOcrData = service.getJsonObject(documentHref + "/serverProcessed/ocrData/sensitiveData");
                Optional<JsonObject> ocrData = Optional.ofNullable(execute(callOcrData));
                
                String ocrDataFileName = "data/" + id + ".ocr.txt";
                ocrData.ifPresent(d -> writeText(ocrDataFileName, d.toString()));
                
                
                
                
                
                Call<JsonObject> callRawImage = service.getJsonObject(documentHref + "/clientCapture/unprocessedImage/sensitiveData");
                JsonObject jsonImage = execute(callRawImage);
                
                if (jsonImage != null) {
                    
                    writeImage("data/" + id + ".jpg", jsonImage.get("value").getAsString());
                }
              
            }
        }
       
        if (jsonIdChecks != null) {
            
            JsonElement next = jsonIdChecks.getAsJsonObject("paging").get("next");

            
            if (next != null) {
                crawl(service, next.getAsString());
            }
            
        }
        
        System.out.println("end of page");
        
    }
    
    
    public static <T> JsonObject execute (Call<T> call) {
        try {
            
            System.out.println(call.request());
            
            Response<T> response = call.execute();
            
            System.out.println(response + "\n");
            
            return (JsonObject) response.body();
        } 
        catch (SocketTimeoutException e) {
           System.out.println("Error: " + e + "\n");
        }
        catch (IOException e) {
            //e.printStackTrace();
        }
        return null;
    }
    
    
    public static void writeImage(String filename, String base64) {

        try {
            byte[] data = Base64.getDecoder().decode(base64);
            try (OutputStream stream = new FileOutputStream(new File(filename))) {
                stream.write(data);
            }

        } catch (Exception e) {

        }
    }
    
    
    public static void writeText(String filename, String text) {
        
        try {
            OutputStream stream = new FileOutputStream(new File(filename));
            stream.write(text.getBytes());
            stream.close();
        } catch (Exception e) {
            
            e.printStackTrace();
        }

    }
}
