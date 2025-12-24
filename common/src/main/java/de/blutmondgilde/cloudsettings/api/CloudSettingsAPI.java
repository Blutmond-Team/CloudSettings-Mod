package de.blutmondgilde.cloudsettings.api;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import de.blutmondgilde.cloudsettings.BuildConstants;
import de.blutmondgilde.cloudsettings.CloudSettings;
import de.blutmondgilde.cloudsettings.api.pojo.BackendSessionTokenRequest;
import de.blutmondgilde.cloudsettings.api.pojo.OptionsResponse;
import de.blutmondgilde.cloudsettings.api.pojo.ServerIdRequest;
import de.blutmondgilde.cloudsettings.api.pojo.ServerIdResponse;
import de.blutmondgilde.cloudsettings.api.pojo.SessionTokenResponse;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.*;

public class CloudSettingsAPI {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = httpClient();
    private static String SESSION_TOKEN = null;

    private static HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static final ScheduledFuture<?> syncTask = executor.scheduleWithFixedDelay(() -> {
        if (!CloudSettings.getStatus().isInitialized() || CloudSettings.getStatus().isErrored()) return;
        Collection<String> settings = CloudSettings.getPendingChanges().values();
        if (settings.isEmpty()) {
            CloudSettings.getLogger().debug("Skipping sync due to no changes.");
            return;
        }

        var request = authorizedRequest("/storage/options").map(builder -> builder.POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(new OptionsResponse(settings.toArray(new String[settings.size()]))))).build());
        if (request.isPresent()) {
            try {
                var response = HTTP_CLIENT.send(request.get(), responseInfo -> HttpResponse.BodySubscribers.ofString(Charset.defaultCharset()));
                if (response.statusCode() != 200) {
                    CloudSettings.getLogger()
                            .error("Error on storing Options in Cloud.\nStatus Code: {}\nStatus Text: {}",
                                    response.statusCode(),
                                    response.body());
                }
                CloudSettings.getLogger().info("Synchronized {} Options with CloudSettings Cloud Storage", settings.size());
                CloudSettings.getPendingChanges().clear();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }, 5, 5, TimeUnit.SECONDS);

    public static CompletableFuture<String[]> getStoredOptions() {
        CompletableFuture<String[]> future = new CompletableFuture<>();

        executor.submit(() -> {
            var request = authorizedRequest("/storage/options").map(HttpRequest.Builder::build);
            if (request.isPresent()) {
                try {
                    var response = HTTP_CLIENT.sendAsync(request.get(), responseInfo -> HttpResponse.BodySubscribers.ofString(Charset.defaultCharset()));
                    OptionsResponse optionsResponse = GSON.fromJson(response.get().body(), OptionsResponse.class);
                    future.complete(optionsResponse.getOptions());
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    future.complete(new String[0]);
                }
            }
        });

        return future;
    }

    private static Optional<HttpRequest.Builder> authorizedRequest(final String url) {
        if (checkLogin()) {
            return Optional.of(
                    HttpRequest.newBuilder()
                            .uri(URI.create(BuildConstants.API_BASE_URL + url))
                            .header(HttpHeaders.AUTHORIZATION, SESSION_TOKEN)
                            .header(HttpHeaders.CONTENT_TYPE, "application/json")
                            .header(HttpHeaders.ACCEPT, "application/json")
                            .header(HttpHeaders.USER_AGENT, BuildConstants.HTTP_USER_AGENT)
            );
        }
        return Optional.empty();
    }

    public static void shutdown() {
        executor.shutdownNow();

        if (!CloudSettings.getStatus().isInitialized() || CloudSettings.getStatus().isErrored()) return;
        Collection<String> settings = CloudSettings.getPendingChanges().values();
        if (settings.size() == 0) {
            CloudSettings.getLogger().debug("Skipping sync due to no changes.");
            return;
        }

        var request = authorizedRequest("/storage/options").map(builder -> builder.POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(new OptionsResponse(settings.toArray(new String[settings.size()]))))).build());
        if (checkLogin() && request.isPresent()) {
            try {
                var response = HTTP_CLIENT.send(request.get(), responseInfo -> HttpResponse.BodySubscribers.discarding());
                if (response.statusCode() != 200) {
                    CloudSettings.getLogger().error("Error on storing Options in Cloud.\nStatus Code: {}", response.statusCode());
                }
                CloudSettings.getLogger().info("Synchronized {} Options with CloudSettings Cloud Storage", settings.size());
                CloudSettings.getPendingChanges().clear();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean checkLogin() {
        if (SESSION_TOKEN != null) return true;
        CloudSettings.getLogger().info("Starting Login...");
        // Get Server Id from backend
        try {
            HttpRequest requestServerId = HttpRequest.newBuilder()
                    .uri(URI.create(BuildConstants.API_BASE_URL + "/auth/serverId"))
                    .header(HttpHeaders.USER_AGENT, BuildConstants.HTTP_USER_AGENT)
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(new ServerIdRequest(CloudSettings.getUser().getName(), CloudSettings.getUser().getProfileId().toString()))))
                    .build();

            CloudSettings.getLogger().info("Requesting Server Id");
            var responseServerId = HTTP_CLIENT.sendAsync(requestServerId, responseInfo -> HttpResponse.BodySubscribers.ofString(Charset.defaultCharset()));
            CloudSettings.getLogger().info("Requested Server Id");
            ServerIdResponse serverIdResponse = GSON.fromJson(responseServerId.get().body(), ServerIdResponse.class);
            CloudSettings.getLogger().info("Resolved Server Id: {}", serverIdResponse);
            if (serverIdResponse == null) throw new IllegalStateException("Invalid Response from Server.");

            // Tell Mojang to log us in
            CloudSettings.getLogger().info("Login in into Mojang Session Server");
            Minecraft.getInstance().services().sessionService().joinServer(CloudSettings.getUser().getProfileId(), CloudSettings.getUser().getAccessToken(), serverIdResponse.getServerId());
            CloudSettings.getLogger().info("Logged in into Mojang Session Server");
            // Tell Backend that we're logged in
            HttpRequest requestSessionToken = HttpRequest.newBuilder()
                    .uri(URI.create(BuildConstants.API_BASE_URL + "/auth/notify"))
                    .header(HttpHeaders.USER_AGENT, BuildConstants.HTTP_USER_AGENT)
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(new BackendSessionTokenRequest(CloudSettings.getUser().getName(), CloudSettings.getUser().getProfileId().toString(), serverIdResponse.getServerId()))))
                    .build();
            CloudSettings.getLogger().info("Requesting Session Token");
            var responseSessionToken = HTTP_CLIENT.sendAsync(requestSessionToken, responseInfo -> HttpResponse.BodySubscribers.ofString(Charset.defaultCharset()));
            CloudSettings.getLogger().info("Requested Session Token");
            SessionTokenResponse sessionTokenResponse = GSON.fromJson(responseSessionToken.get().body(), SessionTokenResponse.class);
            CloudSettings.getLogger().info("Resolved Session Token");

            CloudSettings.getLogger().info("Login Successful");
            SESSION_TOKEN = sessionTokenResponse.getToken();
            return true;
        } catch (Exception e) {
            CloudSettings.getLogger().error("Error on Login", e);
        }

        return false;
    }
}
