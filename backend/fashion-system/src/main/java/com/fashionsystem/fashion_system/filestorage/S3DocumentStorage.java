package com.fashionsystem.fashion_system.filestorage;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/** Cloudflare R2 / Amazon S3 / MinIO qua S3 API. Bucket phải để private (không bật public access). */
public class S3DocumentStorage implements DocumentStorage, AutoCloseable {
    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3DocumentStorage(DocumentStorageProperties.S3 config) {
        if (config.getBucket().isBlank() || config.getAccessKey().isBlank() || config.getSecretKey().isBlank())
            throw new IllegalStateException("document-storage.s3: thiếu bucket / access key / secret key");
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey()));
        Region region = Region.of(config.getRegion().isBlank() ? "auto" : config.getRegion());
        S3Configuration s3Config = S3Configuration.builder().pathStyleAccessEnabled(config.isPathStyle()).build();
        var clientBuilder = S3Client.builder().credentialsProvider(credentials).region(region)
                .serviceConfiguration(s3Config).httpClientBuilder(UrlConnectionHttpClient.builder());
        var presignerBuilder = S3Presigner.builder().credentialsProvider(credentials).region(region)
                .serviceConfiguration(s3Config);
        if (!config.getEndpoint().isBlank()) {
            URI endpoint = URI.create(config.getEndpoint());
            clientBuilder.endpointOverride(endpoint);
            presignerBuilder.endpointOverride(endpoint);
        }
        this.client = clientBuilder.build();
        this.presigner = presignerBuilder.build();
        this.bucket = config.getBucket();
    }

    @Override
    public String provider() {
        return "s3";
    }

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType)
                .contentLength(size).build(), RequestBody.fromInputStream(content, size));
    }

    @Override
    public String signedUrl(String key, Duration ttl, String fileName, String contentType, boolean inline) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key)
                .responseContentType(contentType)
                .responseContentDisposition(ContentDispositions.of(inline, fileName))
                .build();
        return presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(ttl)
                .getObjectRequest(get).build()).url().toString();
    }

    @Override
    public void delete(String key) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public void close() {
        presigner.close();
        client.close();
    }
}
