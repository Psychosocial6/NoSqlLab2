package com.example.nosqllab2.clientLeads;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/lead")
@RestController
public class LeadsController {

    private final LeadsService leadsService;

    public LeadsController(LeadsService leadsService) {
        this.leadsService = leadsService;
    }


    @PostMapping
    public ResponseEntity<Long> createLead(){
        Long leadID = leadsService.createLead();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leadID);
    }

}
