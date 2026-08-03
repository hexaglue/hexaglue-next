package com.acme.clinic.schedule;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class VisitReminders {

    @KafkaListener(topics = "visits")
    public void onVisitBooked(String payload) {
        // Nothing to do here: what the scenario states is where the type sits.
    }
}
