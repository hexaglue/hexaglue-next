package com.acme.clinic.owner;

import com.acme.clinic.notification.VisitNotifier;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OwnerService {

    private final OwnerRepository owners;

    private final VisitNotifier notifier;

    public OwnerService(OwnerRepository owners, VisitNotifier notifier) {
        this.owners = owners;
        this.notifier = notifier;
    }

    public List<Owner> byLastName(String lastName) {
        return owners.findByLastName(lastName);
    }

    public Owner register(Owner owner) {
        Owner saved = owners.save(owner);
        notifier.welcome(saved);
        return saved;
    }
}
