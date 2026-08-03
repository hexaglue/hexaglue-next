package com.acme.clinic.owner;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerRepository extends JpaRepository<Owner, Integer> {

    List<Owner> findByLastName(String lastName);
}
