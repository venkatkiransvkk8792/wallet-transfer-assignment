package com.example.wallet.service;

import com.example.wallet.api.TransferRequest;
import com.example.wallet.api.TransferResponse;

public interface TransferProcessor {
    void validateRequest(TransferRequest request);
    TransferResponse transfer(TransferRequest request);
}
