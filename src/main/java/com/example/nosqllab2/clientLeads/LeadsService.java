package com.example.nosqllab2.clientLeads;

import com.example.nosqllab2.riakservices.RiakCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeadsService {
    private final LeadRepository leads;
    private final RiakCounterService counter;

    public long createLead(String userId) {
        long id = leads.create(userId);
        try {
            counter.recordLeadCreated();
        } catch (DataAccessResourceFailureException exception) {
            log.warn("Lead {} was saved but its Riak counter could not be updated", id, exception);
        }
        return id;
    }
}
