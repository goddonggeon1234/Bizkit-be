package com.caro.bizkit.common.S3.service;

import com.caro.bizkit.common.S3.config.S3Properties;
import com.caro.bizkit.common.S3.dto.PresignedUrlResponse;
import com.caro.bizkit.common.S3.dto.UploadCategory;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final S3Properties s3Properties;


    public PresignedUrlResponse createUploadUrl(String key) {
        return presignPutObject(key);
    }



    public String createObjectKey(UploadCategory type, String originName) {
        String envPrefix = s3Properties.getEnvPrefix();
        String cleanedPrefix = type.prefix();
        String datePath = LocalDate.now().toString().replace("-", "/");
        String extension = extractExtension(originName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        if (StringUtils.hasText(extension)) {
            return envPrefix + "/" + cleanedPrefix + "/" + datePath + "/" + uuid + "." + extension;
        }
        String key = envPrefix + "/" + cleanedPrefix + "/" + datePath + "/" + uuid;
        log.warn("No extension found in originName: {}", originName);
        return key;
    }

    public String getPublicUrl(String key) {
        String bucket = requireBucket();
        String normalizedKey = requireKey(key);
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucket,
                s3Properties.getRegion(),
                normalizedKey);
    }

    public String uploadBytes(String key, byte[] bytes, String contentType) {
        String bucket = requireBucket();
        String normalizedKey = requireKey(key);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(normalizedKey)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(bytes));
        return normalizedKey;
    }

    public void deleteObject(String key) {
        String bucket = requireBucket();
        String normalizedKey = requireKey(key);
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(normalizedKey)
                .build();
        s3Client.deleteObject(request);
    }

    private PresignedUrlResponse presignPutObject(String key) {
        String bucket = requireBucket();
        String normalizedKey = requireKey(key);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(normalizedKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(s3Properties.getPresignedUrlExpirationSeconds()))
                .putObjectRequest(putObjectRequest)
                .build();
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return new PresignedUrlResponse(
                presignedRequest.url().toString(),
                normalizedKey,
                s3Properties.getPresignedUrlExpirationSeconds()
        );
    }

    private String requireBucket() {
        if (!StringUtils.hasText(s3Properties.getBucket())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 bucket is not configured");
        }
        return s3Properties.getBucket();
    }

    private String requireKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "S3 object key is required");
        }
        return key;
    }


    private String extractExtension(String originName) {
        if (!StringUtils.hasText(originName)) {
            return "";
        }
        int lastDotIndex = originName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == originName.length() - 1) {
            return "";
        }
        return originName.substring(lastDotIndex + 1).toLowerCase();
    }
}
