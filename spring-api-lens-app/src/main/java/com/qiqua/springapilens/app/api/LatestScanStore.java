package com.qiqua.springapilens.app.api;

import com.qiqua.springapilens.core.model.ScanResult;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class LatestScanStore {
    private final AtomicReference<ScanResult> latest = new AtomicReference<>();

    public void save(ScanResult scanResult) {
        latest.set(scanResult);
    }

    public Optional<ScanResult> latest() {
        return Optional.ofNullable(latest.get());
    }

    public void clear() {
        latest.set(null);
    }
}
