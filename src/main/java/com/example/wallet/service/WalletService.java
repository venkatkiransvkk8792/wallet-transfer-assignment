package com.example.wallet.service;

import com.example.wallet.api.TransferRequest;
import com.example.wallet.api.TransferResponse;
import com.example.wallet.api.WalletBalanceResponse;
import com.example.wallet.domain.Transfer;
import com.example.wallet.domain.Wallet;
import com.example.wallet.exception.IdempotencyConflictException;
import com.example.wallet.exception.InvalidTransferException;
import com.example.wallet.exception.ValidationException;
import com.example.wallet.exception.WalletNotFoundException;
import com.example.wallet.repository.TransferRepository;
import com.example.wallet.repository.WalletRepository;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class WalletService {
    private final TransferRepository transferRepository;
    private final WalletRepository walletRepository;
    private final TransferTransactionService transactionService;
    private final EntityManager entityManager;

    public WalletService(TransferRepository transferRepository,
                         WalletRepository walletRepository,
                         TransferTransactionService transactionService,
                         EntityManager entityManager) {
        this.transferRepository = transferRepository;
        this.walletRepository=walletRepository;
        this.transactionService = transactionService;
        this.entityManager = entityManager;
    }

    public WalletBalanceResponse getBalance(String walletId) {

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() ->
                        new WalletNotFoundException(walletId));

        return new WalletBalanceResponse(
                wallet.getWalletId(),
                wallet.getBalance());
    }

    public TransferResponse transfer(TransferRequest transferRequest) {
        validateRequest(transferRequest);

        // Fast path for normal retries. The unique DB constraint is still the
        // final authority when two requests with the same key race.
        var existing = transferRepository.findByIdempotencyKey(transferRequest.idempotencyKey());
        if (existing.isPresent()) {
            assertSameRequest(existing.get(), transferRequest);
            return TransferResponse.from(existing.get());
        }

        try {
            return transactionService.processNew(transferRequest);
        } catch (DataIntegrityViolationException race) {
            // The losing concurrent request may have failed on the unique
            // idempotency constraint. Its transaction has already rolled back.
            entityManager.clear();
            var winner = transferRepository.findByIdempotencyKey(transferRequest.idempotencyKey())
                .orElseThrow(() -> race);
            assertSameRequest(winner, transferRequest);
            return TransferResponse.from(winner);
        }
    }

    private void validateRequest(TransferRequest r) {
        if (r.fromWalletId().equals(r.toWalletId())) {
            throw new InvalidTransferException(
                    "fromWalletId and toWalletId must differ");
        }
        if (r.amount().scale() > 2) {
            throw new ValidationException(
                    "amount supports at most 2 decimal places");
        }
    }

    private void assertSameRequest(Transfer t, TransferRequest r) {

        if (!t.getFromWalletId().equals(r.fromWalletId())
            || !t.getToWalletId().equals(r.toWalletId())
            || t.getAmount().compareTo(r.amount()) != 0) {
            throw new IdempotencyConflictException(
                    "Idempotency key was reused with different transfer parameters");
        }
    }
}
