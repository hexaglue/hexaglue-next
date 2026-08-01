package com.example.banking.ports.out;
import com.example.banking.domain.Account;
import com.example.banking.domain.AccountId;
import java.util.Optional;
import org.jmolecules.ddd.annotation.Repository;
@Repository
public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(AccountId id);
    void delete(Account account);
}
