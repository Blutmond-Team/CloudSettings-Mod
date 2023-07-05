package de.blutmondgilde.cloudsettings.api;

import com.google.gson.Gson;
import com.mojang.authlib.exceptions.AuthenticationException;
import de.blutmondgilde.cloudsettings.CloudSettings;
import de.blutmondgilde.cloudsettings.api.pojo.BackendSessionTokenRequest;
import de.blutmondgilde.cloudsettings.api.pojo.OptionsResponse;
import de.blutmondgilde.cloudsettings.api.pojo.ServerIdRequest;
import de.blutmondgilde.cloudsettings.api.pojo.ServerIdResponse;
import de.blutmondgilde.cloudsettings.api.pojo.SessionTokenResponse;
import net.minecraft.client.Minecraft;
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
    private static String SESSION_TOKEN = null;

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
    //private static final String baseUrl = "http://localhost:3000/api/v1";
    private static final String baseUrl = "https://cloudsettings.blutmondgilde.de/api/v1";
    private static final ScheduledFuture<?> syncTask = executor.scheduleWithFixedDelay(() -> {
        if (!CloudSettings.getStatus().isInitialized() || CloudSettings.getStatus().isErrored()) return;
        Collection<String> settings = CloudSettings.getPendingChanges().values();
        if (settings.size() == 0) {
            CloudSettings.getLogger().debug("Skipping sync due to no changes.");
            return;
        }

        HttpPost request = post("/storage/options");
        if (request != null) {
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
    }, 5, 5, TimeUnit.SECONDS);

    public static CompletableFuture<String[]> getStoredOptions() {
        CompletableFuture<String[]> future = new CompletableFuture<>();

        executor.submit(() -> {
            HttpGet request = get("/storage/options");
            if (request != null) {
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
            }
        });

        return future;
    }

    private static HttpGet get(final String url) {
        if (checkLogin()) {
            HttpGet get = new HttpGet(baseUrl + url);
            get.addHeader(HttpHeaders.AUTHORIZATION, SESSION_TOKEN);
            get.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            get.addHeader(HttpHeaders.ACCEPT, "application/json");
            get.addHeader(HttpHeaders.USER_AGENT, String.format("cloud settings mod (1.19 %s)", CloudSettings.MOD_VERSION));
            return get;
        }

        return null;
    }

    private static HttpPost post(final String url) {
        if (checkLogin()) {
            HttpPost post = new HttpPost(baseUrl + url);
            post.addHeader(HttpHeaders.AUTHORIZATION, SESSION_TOKEN);
            post.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            post.addHeader(HttpHeaders.ACCEPT, "application/json");
            post.addHeader(HttpHeaders.USER_AGENT, String.format("cloud settings mod (1.19 %s)", CloudSettings.MOD_VERSION));
            return post;
        }
        return null;
    }

    private static <T> T resolveJsonBody(CloseableHttpResponse response, Class<T> pojoClass) throws IOException {
        HttpEntity entity = response.getEntity();
        String responseBody = EntityUtils.toString(entity);
        //CloudSettings.getLogger().debug("Resolved API response to body:\n{}", responseBody);
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
        if (checkLogin()) {
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

    private static boolean checkLogin() {
        if (SESSION_TOKEN != null) return true;
        CloudSettings.getLogger().info("Starting Login...");
        // Get Server Id from backend
        try {
            HttpPost requestServerId = new HttpPost(baseUrl + "/auth/serverId");
            StringEntity body = new StringEntity(GSON.toJson(new ServerIdRequest(CloudSettings.getUser().getName(), CloudSettings.getUser().getUuid())));
            requestServerId.setEntity(body);

            CloudSettings.getLogger().info("Requesting Server Id");
            CloseableHttpResponse responseServerId = HTTP_CLIENT.execute(requestServerId);
            CloudSettings.getLogger().info("Requested Server Id");
            ServerIdResponse serverIdResponse = resolveJsonBody(responseServerId, ServerIdResponse.class);
            CloudSettings.getLogger().info("Resolved Server Id");
            responseServerId.close();
            // Tell Mojang to log us in
            CloudSettings.getLogger().info("Login in into Mojang Session Server");
            Minecraft.getInstance().getMinecraftSessionService().joinServer(CloudSettings.getUser().getGameProfile(), CloudSettings.getUser().getAccessToken(), serverIdResponse.getServerId());
            CloudSettings.getLogger().info("Logged in into Mojang Session Server");
            // Tell Backend that we're logged in
            HttpPost requestSessionToken = new HttpPost(baseUrl + "/auth/notify");
            requestSessionToken.setEntity(new StringEntity(GSON.toJson(new BackendSessionTokenRequest(CloudSettings.getUser().getName(), CloudSettings.getUser().getUuid(), serverIdResponse.getServerId()))));
            CloudSettings.getLogger().info("Requesting Session Token");
            CloseableHttpResponse responseSessionToken = HTTP_CLIENT.execute(requestSessionToken);
            CloudSettings.getLogger().info("Requested Session Token");
            SessionTokenResponse sessionTokenResponse = resolveJsonBody(responseSessionToken, SessionTokenResponse.class);
            CloudSettings.getLogger().info("Resolved Session Token");
            responseSessionToken.close();

            CloudSettings.getLogger().info("Login Successful");
            SESSION_TOKEN = sessionTokenResponse.getToken();
            return true;
        } catch (IOException | AuthenticationException e) {
            CloudSettings.getLogger().error("Error on Login", e);
        }

        return false;
    }
}
