package com.example.wallet.service;

import com.example.wallet.api.TransferRequest;
import com.example.wallet.api.TransferResponse;
import com.example.wallet.domain.LedgerEntry;
import com.example.wallet.domain.LedgerEntryType;
import com.example.wallet.domain.Transfer;
import com.example.wallet.domain.Wallet;
import com.example.wallet.exception.WalletNotActiveException;
import com.example.wallet.exception.WalletNotFoundException;
import com.example.wallet.repository.LedgerEntryRepository;
import com.example.wallet.repository.TransferRepository;
import com.example.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferTransactionService {
    private final TransferRepository transfers;
    private final WalletRepository wallets;
    private final LedgerEntryRepository ledger;

    public TransferTransactionService(TransferRepository transfers,
                                      WalletRepository wallets,
                                      LedgerEntryRepository ledger) {
        this.transfers = transfers;
        this.wallets = wallets;
        this.ledger = ledger;
    }

    @Transactional
    public TransferResponse processNew(TransferRequest transferRequest) {
        var transfer = new Transfer(
                transferRequest.idempotencyKey(),
                transferRequest.fromWalletId(),
                transferRequest.toWalletId(),
                transferRequest.amount()
        );
        transfers.saveAndFlush(transfer);

        // Always lock both wallets in the same order. This prevents deadlocks when
        // wallet_1 -> wallet_2 and wallet_2 -> wallet_1 execute concurrently.
        String firstId = transferRequest.fromWalletId().compareTo(transferRequest.toWalletId()) < 0
            ? transferRequest.fromWalletId() : transferRequest.toWalletId();
        String secondId = firstId.equals(transferRequest.fromWalletId())
            ? transferRequest.toWalletId() : transferRequest.fromWalletId();

        Wallet first = wallets.findByWalletId(firstId)
                .orElseThrow(() ->
                        new WalletNotFoundException(firstId));

        Wallet second = wallets.findByWalletId(secondId)
                .orElseThrow(() ->
                        new WalletNotFoundException(secondId));

        Wallet source = first.getWalletId().equals(transferRequest.fromWalletId()) ? first : second;
        Wallet destination = first.getWalletId().equals(transferRequest.toWalletId()) ? first : second;

        // status validation
        validateWalletStatus(source, destination);

        if (source.getBalance().compareTo(transferRequest.amount()) < 0) {
            transfer.markFailed();
            transfers.save(transfer);
            return TransferResponse.from(transfer);
        }

        source.debit(transferRequest.amount());
        destination.credit(transferRequest.amount());

        ledger.save(new LedgerEntry(source.getWalletId(), transfer.getId(), LedgerEntryType.DEBIT, transferRequest.amount()));
        ledger.save(new LedgerEntry(destination.getWalletId(), transfer.getId(), LedgerEntryType.CREDIT, transferRequest.amount()));

        transfer.markProcessed();
        transfers.save(transfer);
        return TransferResponse.from(transfer);
    }

    private void validateWalletStatus(
            Wallet source,
            Wallet destination
    ) {
        if (!"ACTIVE".equals(source.getStatus())) {
            throw new WalletNotActiveException(
                    "Source wallet is not active: " + source.getWalletId()
            );
        }

        if (!"ACTIVE".equals(destination.getStatus())) {
            throw new WalletNotActiveException(
                    "Destination wallet is not active: " + destination.getWalletId()
            );
        }
    }
}
