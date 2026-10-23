package com.example.nosqllab2.clientLeads;

import com.example.nosqllab2.riakservices.RiakCounterService;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class LeadsService {

    private final RiakCounterService riakCounterService;

    public LeadsService(RiakCounterService riakCounterService) {
        this.riakCounterService = riakCounterService;
    }

    public long createLead(){
        try {
            return riakCounterService.generateNextId(LeadsService.class);
        } catch (ExecutionException |InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка при создании заявки: " + e.getCause().getMessage(), e);
        }
    }

}
