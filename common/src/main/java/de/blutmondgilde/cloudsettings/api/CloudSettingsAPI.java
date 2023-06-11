package de.blutmondgilde.cloudsettings.api;

import com.google.gson.Gson;
import de.blutmondgilde.cloudsettings.CloudSettings;
import de.blutmondgilde.cloudsettings.api.pojo.OptionsResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CloudSettingsAPI {
    private static final Gson GSON = new Gson();
    private static final CloseableHttpClient HTTP_CLIENT = httpClient();

    private static CloseableHttpClient httpClient() {
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, (x509Certificates, s) -> true);
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), (hostname, session) -> true);
            CloseableHttpClient client = HttpClients.custom().setSSLSocketFactory(sslsf).build();
            return client;
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    //private static final String baseUrl = "http://localhost:3000/api";
    private static final String baseUrl = "https://cloudsettings.blutmondgilde.de/api";
    private static final ScheduledFuture<?> syncTask = executor.scheduleWithFixedDelay(() -> {
        if (!CloudSettings.getStatus().isInitialized() || CloudSettings.getStatus().isErrored()) return;
        Collection<String> settings = CloudSettings.getPendingChanges().values();
        if (settings.size() == 0) {
            CloudSettings.getLogger().debug("Skipping sync due to no changes.");
            return;
        }

        HttpPost request = post("/storage/options");
        try {
            StringEntity body = new StringEntity(GSON.toJson(new OptionsResponse(settings.toArray(new String[settings.size()]))));
            request.setEntity(body);
            try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    CloudSettings.getLogger()
                            .error("Error on storing Options in Cloud.\nStatus Code: {}\nStatus Text: {}",
                                    response.getStatusLine().getStatusCode(),
                                    response.getStatusLine().getReasonPhrase());
                }
            }
            CloudSettings.getLogger().info("Synchronized {} Options with CloudSettings Cloud Storage", settings.size());
            CloudSettings.getPendingChanges().clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }, 5, 5, TimeUnit.SECONDS);

    public static CompletableFuture<String[]> getStoredOptions() {
        CompletableFuture<String[]> future = new CompletableFuture<>();

        executor.submit(() -> {
            HttpGet request = get("/storage/options");
            try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    OptionsResponse optionsResponse = resolveJsonBody(response, OptionsResponse.class);
                    future.complete(optionsResponse.getOptions());
                } else {
                    CloudSettings.getLogger()
                            .error("Error on loading Options from Cloud.\nStatus Code: {}\nStatus Text: {}",
                                    response.getStatusLine().getStatusCode(),
                                    response.getStatusLine().getReasonPhrase());
                }
            } catch (IOException e) {
                e.printStackTrace();
                future.complete(new String[0]);
            }
        });

        return future;
    }

    private static HttpGet get(final String url) {
        HttpGet get = new HttpGet(baseUrl + url);
        get.addHeader(HttpHeaders.AUTHORIZATION, CloudSettings.getUser().getAccessToken());
        get.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        get.addHeader(HttpHeaders.ACCEPT, "application/json");
        get.addHeader(HttpHeaders.USER_AGENT, String.format("cloud settings mod (1.20 %s)", CloudSettings.MOD_VERSION));
        return get;
    }

    private static HttpPost post(final String url) {
        HttpPost post = new HttpPost(baseUrl + url);
        post.addHeader(HttpHeaders.AUTHORIZATION, CloudSettings.getUser().getAccessToken());
        post.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        post.addHeader(HttpHeaders.ACCEPT, "application/json");
        post.addHeader(HttpHeaders.USER_AGENT, String.format("cloud settings mod (1.20 %s)", CloudSettings.MOD_VERSION));
        return post;
    }

    private static <T> T resolveJsonBody(CloseableHttpResponse response, Class<T> pojoClass) throws IOException {
        HttpEntity entity = response.getEntity();
        String responseBody = EntityUtils.toString(entity);
        return GSON.fromJson(responseBody, pojoClass);
    }

    public static void shutdown() {
        executor.shutdownNow();

        if (!CloudSettings.getStatus().isInitialized() || CloudSettings.getStatus().isErrored()) return;
        Collection<String> settings = CloudSettings.getPendingChanges().values();
        if (settings.size() == 0) {
            CloudSettings.getLogger().debug("Skipping sync due to no changes.");
            return;
        }

        HttpPost request = post("/storage/options");
        try {
            StringEntity body = new StringEntity(GSON.toJson(new OptionsResponse(settings.toArray(new String[settings.size()]))));
            request.setEntity(body);
            try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    CloudSettings.getLogger()
                            .error("Error on storing Options in Cloud.\nStatus Code: {}\nStatus Text: {}",
                                    response.getStatusLine().getStatusCode(),
                                    response.getStatusLine().getReasonPhrase());
                }
            }
            CloudSettings.getLogger().info("Synchronized {} Options with CloudSettings Cloud Storage", settings.size());
            CloudSettings.getPendingChanges().clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
