package com.zyntral.common.storage;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

/**
 * Thin S3-compatible object storage (AWS S3, MinIO, Coolify S3). Disabled (no-op) until the
 * {@code zyntral.storage.s3.*} env vars are set, so the app boots fine without it.
 */
@Service
public class StorageService {

    private final String bucket;
    private final boolean enabled;
    private final S3Client s3;
    private final S3Presigner presigner;

    public StorageService(
            @Value("${zyntral.storage.s3.endpoint:}") String endpoint,
            @Value("${zyntral.storage.s3.region:us-east-1}") String region,
            @Value("${zyntral.storage.s3.bucket:}") String bucket,
            @Value("${zyntral.storage.s3.access-key:}") String accessKey,
            @Value("${zyntral.storage.s3.secret-key:}") String secretKey) {
        this.bucket = bucket;
        this.enabled = !bucket.isBlank() && !accessKey.isBlank();
        if (enabled) {
            var creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
            var cfg = S3Configuration.builder().pathStyleAccessEnabled(true).build();
            var s3b = S3Client.builder()
                    .httpClient(UrlConnectionHttpClient.create())
                    .region(Region.of(region)).credentialsProvider(creds).serviceConfiguration(cfg);
            var pb = S3Presigner.builder()
                    .region(Region.of(region)).credentialsProvider(creds).serviceConfiguration(cfg);
            if (!endpoint.isBlank()) {
                s3b.endpointOverride(URI.create(endpoint));
                pb.endpointOverride(URI.create(endpoint));
            }
            this.s3 = s3b.build();
            this.presigner = pb.build();
        } else {
            this.s3 = null;
            this.presigner = null;
        }
    }

    public boolean isEnabled() { return enabled; }

    public void put(String key, byte[] bytes, String contentType) {
        requireEnabled();
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(bytes));
    }

    public String presignedGetUrl(String key, Duration ttl) {
        requireEnabled();
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        return presigner.presignGetObject(req).url().toString();
    }

    private void requireEnabled() {
        if (!enabled) throw new IllegalStateException("S3 storage is not configured");
    }

    @PreDestroy
    void close() {
        if (s3 != null) s3.close();
        if (presigner != null) presigner.close();
    }
}
