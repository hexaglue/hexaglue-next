package com.acme.clinic.owner;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerService {

    private final OwnerRepository owners;

    public OwnerService(OwnerRepository owners) {
        this.owners = owners;
    }

    public List<Owner> byLastName(String lastName) {
        return owners.findByLastName(lastName);
    }

    @Transactional
    public Owner register(Owner owner) {
        return owners.save(owner);
    }
}
