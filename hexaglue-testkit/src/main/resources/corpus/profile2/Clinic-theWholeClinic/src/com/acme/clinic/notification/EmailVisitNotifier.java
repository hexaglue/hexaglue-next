package com.acme.clinic.notification;

import com.acme.clinic.owner.Owner;
import org.springframework.web.client.RestTemplate;

public class EmailVisitNotifier implements VisitNotifier {

    private final RestTemplate mailer;

    public EmailVisitNotifier(RestTemplate mailer) {
        this.mailer = mailer;
    }

    @Override
    public void welcome(Owner owner) {
        mailer.postForObject("/mail", owner, Void.class);
    }
}
