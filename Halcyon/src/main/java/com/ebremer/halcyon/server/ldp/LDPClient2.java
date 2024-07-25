package com.ebremer.halcyon.server.ldp;

import javax.net.ssl.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class LDPClient2 {

    public static void main(String[] args) throws Exception {
        String url = "https://localhost:8888/ldp/annotations/wow2.json";
       // String url = "https://beak.bmi.stonybrook.edu:8889/ldp/erich/wow5.json";
        //String url = "https://beak.bmi.stonybrook.edu:8889/ldp/Storage/images/tcga_data/brca/testing2.json";

        String filePath = "D:\\dicom\\0002.json";
        String body = new String(Files.readAllBytes(Paths.get(filePath)));

        HttpClient client = HttpClient.newBuilder()
            .sslContext(getInsecureSslContext())
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(url))
            .header("Content-Type", "application/json")
            .PUT(BodyPublishers.ofString(body, StandardCharsets.UTF_8))
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
