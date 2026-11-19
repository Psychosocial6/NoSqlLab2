package com.example.nosqllab2.clientLeads;

import com.example.nosqllab2.notifications.WebSocketDeliveryService;
import com.example.nosqllab2.riakservices.RiakCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeadsService {
    private final RiakCounterService counter;
    private final WebSocketDeliveryService notifications;

    public long createLead(String userId) {
        long number = counter.incrementLeadCounter();
        try {
            notifications.send(userId, "Заявка №" + number + " принята");
        } catch (DataAccessException exception) {
            log.warn("Could not save notification for lead number {}", number, exception);
        }
        return number;
    }
}
