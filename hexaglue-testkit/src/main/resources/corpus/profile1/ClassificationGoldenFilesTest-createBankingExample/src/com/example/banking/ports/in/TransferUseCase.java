package com.example.banking.ports.in;
import com.example.banking.domain.AccountId;
import com.example.banking.domain.Money;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
@PrimaryPort
public interface TransferUseCase {
    void transfer(AccountId from, AccountId to, Money amount);
}
