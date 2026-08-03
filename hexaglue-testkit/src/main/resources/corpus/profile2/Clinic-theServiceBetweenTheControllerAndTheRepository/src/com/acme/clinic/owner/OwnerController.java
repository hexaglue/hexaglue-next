package com.acme.clinic.owner;

import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OwnerController {

    private final OwnerService service;

    public OwnerController(OwnerService service) {
        this.service = service;
    }

    public List<Owner> search(String lastName) {
        return service.byLastName(lastName);
    }
}
