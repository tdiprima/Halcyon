package com.ebremer.halcyon.server.ldp;

import javax.net.ssl.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class LDPClient {

    public static void main(String[] args) throws Exception {
        String url = "https://localhost:8888/ldp/annotations/wow"; // Change to your LDP server URL
        String jsonPayload = "{\"key\": \"value\"}";
        String turtlePayload = "@prefix ex: <http://example.com/> . ex:subject ex:predicate ex:object .";

        String boundary = "===" + System.currentTimeMillis() + "===";

        String body =
            "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"json\"\r\n" +
            "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
            jsonPayload + "\r\n" +
            "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"rdf\"\r\n" +
            "Content-Type: text/turtle; charset=UTF-8\r\n\r\n" +
            turtlePayload + "\r\n" +
            "--" + boundary + "--\r\n";

        HttpClient client = HttpClient.newBuilder()
            .sslContext(getInsecureSslContext())
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(url))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response Code: " + response.statusCode());
        System.out.println("Response: " + response.body());
    }

    private static SSLContext getInsecureSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        return sslContext;
    }
}
