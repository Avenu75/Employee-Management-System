package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rainbow.springboot.thymeleafdemo.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    Optional<Permission> findByCode(String code);

    List<Permission> findAllByOrderByCategoryAscDisplayNameAsc();

    boolean existsByCode(String code);
}
