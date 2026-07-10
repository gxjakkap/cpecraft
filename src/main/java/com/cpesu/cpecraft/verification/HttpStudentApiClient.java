package com.cpesu.cpecraft.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.cpesu.cpecraft.Cpecraft;

public final class HttpStudentApiClient implements StudentApiClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final URI baseUri;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpStudentApiClient(URI baseUri, String apiKey) {
        this.baseUri = baseUri;
        this.apiKey = apiKey;
    }

    @Override
    public CompletableFuture<Optional<StudentInfo>> verify(String studentId) {
        URI uri = URI.create(String.format("%s/api/yb/get-info/%s", this.baseUri, studentId));
        // Note: never log this.apiKey - it's a secret, sent only in the Authorization header below.
        Cpecraft.LOGGER.info("Calling YB API: GET {}", uri);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", this.apiKey)
                .build();

        return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    Cpecraft.LOGGER.info("YB API responded {} for studentId='{}'", resp.statusCode(), studentId);
                    if (resp.statusCode() != 200) {
                        Cpecraft.LOGGER.warn("YB API returned non-200 status {} for studentId='{}'", resp.statusCode(), studentId);
                        return Optional.<StudentInfo>empty();
                    }
                    try {
                        JsonNode root = mapper.readTree(resp.body());
                        JsonNode data = root.get("data");
                        if (data == null) {
                            Cpecraft.LOGGER.warn("YB API response for studentId='{}' had no 'data' field: {}", studentId, resp.body());
                            return Optional.<StudentInfo>empty();
                        }
                        StudentInfo info = new StudentInfo(
                                data.get("id").asText(),
                                data.get("eng_name").asText(),
                                data.get("eng_nick").asText(),
                                data.get("gen").asText()
                        );
                        Cpecraft.LOGGER.info("YB API matched studentId='{}' -> name='{}', nickName='{}', batch='{}'",
                                studentId, info.name(), info.nickName(), info.batch());
                        return Optional.of(info);
                    } catch (Exception e) {
                        Cpecraft.LOGGER.warn("Failed to parse YB API response for studentId='{}': {}", studentId, resp.body(), e);
                        return Optional.<StudentInfo>empty();
                    }
                });
    }
}
