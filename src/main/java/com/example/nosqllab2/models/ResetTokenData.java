package com.example.nosqllab2.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResetTokenData {
    private String username;
    private String token;
    private Long expiresAt;

    @JsonIgnore
    public boolean isExpired() {
        return Instant.now().getEpochSecond() > this.expiresAt;
    }
}