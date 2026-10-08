package com.example.nosqllab2.resetToken;

import com.example.nosqllab2.models.ResetTokenData;
import com.example.nosqllab2.repository.ResetTokenDataRepository;
import com.example.nosqllab2.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ResetTokenService {

    private final ResetTokenDataRepository resetTokenDataRepository;
    private final UserRepository userRepository;


    private static final long TTL_SECONDS = 60;
    private static final String BASE_URL = "http://localhost:8080/api/resetData/reset";

    public String createResetToken(String username){
        if(Boolean.FALSE.equals(userRepository.existsByNameIgnoreCase(username).block())){
            throw new RuntimeException("User not found");
        }

        String token = UUID.randomUUID().toString();
        long expiresAt = Instant.now().getEpochSecond() + TTL_SECONDS;

        ResetTokenData resetTokenData = new ResetTokenData(username, token, expiresAt);

        resetTokenDataRepository.save(resetTokenData);

        return BASE_URL + "?username=" + username + "&token=" + token;
    }

    public String activateToken (String token, String username){
        ResetTokenData resetTokenData = resetTokenDataRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("token not found"));

        if (!resetTokenData.getToken().equals(token)){
            throw new RuntimeException("Invalid token");
        }

        if (resetTokenData.isExpired()){
            resetTokenDataRepository.delete(username);
            throw new RuntimeException("token outdated(");
        }

        resetTokenDataRepository.delete(username);
        return "Данные успешно обновлены";
    }


}
