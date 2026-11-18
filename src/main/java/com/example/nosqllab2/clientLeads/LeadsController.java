package com.example.nosqllab2.clientLeads;

import com.example.nosqllab2.users.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/lead")
@RestController
public class LeadsController {
    private final LeadsService leadsService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public long createLead(@AuthenticationPrincipal UserPrincipal principal) {
        return leadsService.createLead(principal.id());
    }
}
