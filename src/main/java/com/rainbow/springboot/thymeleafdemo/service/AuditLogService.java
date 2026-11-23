package com.rainbow.springboot.thymeleafdemo.service;

import java.util.List;

import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.SecurityAuditLog;

public interface AuditLogService {
    void record(String action, AppUser actor, AppUser target, String details);
    void record(String action, String actorUsername, String targetUsername, String details);
    List<SecurityAuditLog> findRecentEntries();
    void deleteEntry(int entryId);
    void deleteAll();
}
