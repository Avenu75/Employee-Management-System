package com.rainbow.springboot.thymeleafdemo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rainbow.springboot.thymeleafdemo.dao.SecurityAuditLogRepository;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.SecurityAuditLog;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final SecurityAuditLogRepository securityAuditLogRepository;

    public AuditLogServiceImpl(SecurityAuditLogRepository theSecurityAuditLogRepository) {
        securityAuditLogRepository = theSecurityAuditLogRepository;
    }

    @Override
    public void record(String action, AppUser actor, AppUser target, String details) {
        String actorUsername = actor != null ? actor.getUsername() : "system";
        String targetUsername = target != null ? target.getUsername() : null;
        securityAuditLogRepository.save(new SecurityAuditLog(action, actorUsername, targetUsername, details));
    }

    @Override
    public void record(String action, String actorUsername, String targetUsername, String details) {
        securityAuditLogRepository.save(new SecurityAuditLog(action,
                actorUsername != null ? actorUsername : "system",
                targetUsername,
                details));
    }

    @Override
    public List<SecurityAuditLog> findRecentEntries() {
        return securityAuditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public void deleteEntry(int entryId) {
        securityAuditLogRepository.deleteById(entryId);
    }

    @Override
    public void deleteAll() {
        securityAuditLogRepository.deleteAll();
    }
}
