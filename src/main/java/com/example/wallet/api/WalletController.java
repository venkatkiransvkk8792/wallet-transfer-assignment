package com.example.wallet.api;

import com.example.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/wallet/v1")
public class WalletController {
    private final WalletService walletService;
    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);

    public WalletController(WalletService service) {
        this.walletService = service;
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest transferRequest) {
        logger.info("Processing transfer request: {}", transferRequest);
        return ResponseEntity.ok(walletService.transfer(transferRequest));
    }

    @GetMapping("/{walletId}/balance")
    public ResponseEntity<WalletBalanceResponse> getBalance(
            @PathVariable String walletId) {

        logger.info("Processing get balance request for wallet: {}", walletId);
        return ResponseEntity.ok(walletService.getBalance(walletId));
    }
}
