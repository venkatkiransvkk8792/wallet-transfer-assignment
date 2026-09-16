package com.example.wallet.api;

import com.example.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet/v1")
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService service) {
        this.walletService = service;
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest transferRequest) {
        System.out.println(transferRequest);
        return ResponseEntity.ok(walletService.transfer(transferRequest));
    }

    @GetMapping("/{walletId}/balance")
    public ResponseEntity<WalletBalanceResponse> getBalance(
            @PathVariable String walletId) {

        return ResponseEntity.ok(walletService.getBalance(walletId));
    }
}
