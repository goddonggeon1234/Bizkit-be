package com.caro.bizkit.common.S3.config;

import com.caro.bizkit.common.S3.dto.UploadCategory;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.LifecycleExpiration;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final S3Properties s3Properties;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public ApplicationRunner s3LifecycleConfigurer(S3Client s3Client) {
        return args -> {
            String envPrefix = s3Properties.getEnvPrefix();
            List<LifecycleRule> rules = Arrays.stream(UploadCategory.values())
                    .filter(c -> c.lifetimeDays() != null)
                    .map(c -> LifecycleRule.builder()
                            .id("lifecycle-" + c.prefix())
                            .status(ExpirationStatus.ENABLED)
                            .filter(LifecycleRuleFilter.builder()
                                    .prefix(envPrefix + "/" + c.prefix() + "/")
                                    .build())
                            .expiration(LifecycleExpiration.builder()
                                    .days(c.lifetimeDays())
                                    .build())
                            .build())
                    .toList();

            if (!rules.isEmpty()) {
                s3Client.putBucketLifecycleConfiguration(
                        PutBucketLifecycleConfigurationRequest.builder()
                                .bucket(s3Properties.getBucket())
                                .lifecycleConfiguration(BucketLifecycleConfiguration.builder()
                                        .rules(rules)
                                        .build())
                                .build());
            }
        };
    }
}
