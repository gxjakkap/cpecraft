package com.cpesu.cpecraft.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/yb/get-info?id=%s", this.baseUri, studentId)))
                .header("Authorization", this.apiKey)
                .build();

        return httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() != 200) {
                        return Optional.<StudentInfo>empty();
                    }
                    try {
                        JsonNode root = mapper.readTree(resp.body());
                        JsonNode data = root.get("data");
                        if (data == null) {
                            return Optional.<StudentInfo>empty();
                        }
                        StudentInfo info = new StudentInfo(
                                data.get("id").asText(),
                                data.get("eng_name").asText(),
                                data.get("eng_nick").asText(),
                                data.get("gen").asText()
                        );
                        return Optional.of(info);
                    } catch (Exception e) {
                        return Optional.<StudentInfo>empty();
                    }
                });
    }
}
