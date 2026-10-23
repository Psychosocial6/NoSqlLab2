package com.example.nosqllab2.configuration;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class FirestoreNativeConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.credentials.location}")
    private Resource credentialsResource;

    @Bean
    public Firestore firestore() throws IOException {
        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(credentialsResource.getInputStream()))
                .build();
        return options.getService();
    }
}