package com.qiqua.springapilens.app.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qiqua.springapilens.core.model.ScanResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class ScanHistoryStore {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final DateTimeFormatter ID_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        .withZone(ZoneOffset.UTC);

    private final Path historyDirectory;

    public ScanHistoryStore(Path historyDirectory) {
        this.historyDirectory = historyDirectory;
    }

    public ScanHistoryEntry save(ScanResult result) {
        try {
            Files.createDirectories(historyDirectory);
            Instant scannedAt = Instant.now();
            String id = ID_TIME_FORMAT.format(scannedAt) + "-" + UUID.randomUUID().toString().substring(0, 8);
            ScanHistoryEntry entry = new ScanHistoryEntry(
                id,
                scannedAt,
                result.repositoryInfo().repoName(),
                result.repositoryInfo().rootPath().toString(),
                result.repositoryInfo().currentBranch(),
                result.repositoryInfo().headCommit(),
                result.endpoints().size(),
                result.callEdges().size(),
                result.sqlFragments().size()
            );
            OBJECT_MAPPER.writeValue(metadataPath(id).toFile(), entry);
            OBJECT_MAPPER.writeValue(resultPath(id).toFile(), result);
            return entry;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to save scan history in " + historyDirectory, exception);
        }
    }

    public List<ScanHistoryEntry> list() {
        if (!Files.exists(historyDirectory)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(historyDirectory)) {
            return paths
                .filter(path -> path.getFileName().toString().endsWith(".meta.json"))
                .map(this::readEntry)
                .sorted(Comparator.comparing(ScanHistoryEntry::scannedAt).reversed())
                .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to list scan history in " + historyDirectory, exception);
        }
    }

    public Optional<ScanResult> load(String id) {
        Path path = resultPath(id);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(OBJECT_MAPPER.readValue(path.toFile(), ScanResult.class));
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load scan history " + id, exception);
        }
    }

    private ScanHistoryEntry readEntry(Path path) {
        try {
            return OBJECT_MAPPER.readValue(path.toFile(), ScanHistoryEntry.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read scan history entry " + path, exception);
        }
    }

    private Path metadataPath(String id) {
        return historyDirectory.resolve(id + ".meta.json");
    }

    private Path resultPath(String id) {
        return historyDirectory.resolve(id + ".result.json");
    }
}
