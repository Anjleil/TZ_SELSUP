import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final String API_BASE_URL = "https://ismp.crpt.ru/api/v3";
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicInteger requestCount;
    private final int requestLimit;
    private final long requestInterval;
    private long lastRequestTime;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.requestCount = new AtomicInteger(0);
        this.requestLimit = requestLimit;
        this.requestInterval = timeUnit.toMillis(1);
        this.lastRequestTime = 0;
    }

    public synchronized void createDocument(Object document, String signature) throws Exception {
        checkRequestLimit();
        String documentJson = objectMapper.writeValueAsString(document);
        RequestBody requestBody = new FormBody.Builder()
                .add("document", documentJson)
                .add("signature", signature)
                .build();
        Request request = new Request.Builder()
                .url(API_BASE_URL + "create_document")
                .post(requestBody)
                .build();
        Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            assert response.body() != null;
            throw new Exception("Failed to create document: " + response.code() + " - " + response.body().string());
        }
    }

    private void checkRequestLimit() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRequestTime > requestInterval) {
            requestCount.set(0);
            lastRequestTime = currentTime;
        }
        int currentCount = requestCount.incrementAndGet();
        if (currentCount > requestLimit) {
            long sleepTime = lastRequestTime + requestInterval - currentTime;
            if (sleepTime > 0) {
                Thread.sleep(sleepTime);
                checkRequestLimit();
            } else {
                requestCount.set(0);
                lastRequestTime = currentTime;
            }
        }
    }

    /* Пример использования
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 10);
        try {
            api.createDocument(document, signature);
        } catch (Exception e) {
            ...
        }
     */

}
