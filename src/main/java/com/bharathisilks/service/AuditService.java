package com.bharathisilks.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * In-memory audit trail of sensitive changes (newest first, bounded). Resets on
 * restart — swap the backing store for a table once a durable database is added.
 */
@Service
public class AuditService {

    public record Entry(long time, String actor, String action, String entity, String ref, String detail) {
    }

    private static final int MAX = 500;
    private final ConcurrentLinkedDeque<Entry> log = new ConcurrentLinkedDeque<>();

    public void record(String action, String entity, String ref, String detail) {
        log.addFirst(new Entry(System.currentTimeMillis(), actor(), action, entity,
                ref == null ? "" : ref, detail == null ? "" : detail));
        while (log.size() > MAX) {
            log.pollLast();
        }
    }

    public List<Entry> recent() {
        return new ArrayList<>(log);
    }

    public void clear() {
        log.clear();
    }

    private static String actor() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.getName() != null) ? a.getName() : "system";
    }
}
