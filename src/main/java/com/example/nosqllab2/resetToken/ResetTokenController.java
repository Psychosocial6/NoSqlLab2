package com.example.nosqllab2.resetToken;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/resetData")
@RestController
public class ResetTokenController {

    private final ResetTokenService resetTokenService;

    @PostMapping("/createToken")
    public ResponseEntity<String> createToken(@RequestParam(name="username") String username){
        String url = resetTokenService.createResetToken(username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(url);
    }


    @PostMapping("/reset")
    public ResponseEntity<String> resetData(@RequestParam(name="token") String token, @RequestParam(name="username") String username){
        String response = resetTokenService.activateToken(token, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }


}
