package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rainbow.springboot.thymeleafdemo.entity.SecurityAuditLog;

public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, Integer> {
    List<SecurityAuditLog> findAllByOrderByCreatedAtDesc();
}
