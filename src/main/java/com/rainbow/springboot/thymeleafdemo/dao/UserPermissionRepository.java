package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Permission;
import com.rainbow.springboot.thymeleafdemo.entity.UserPermission;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Integer> {

    /** All currently-granted permissions for one user - used by CustomUserDetailsService
     *  at login time to build that user's Spring Security authorities. */
    List<UserPermission> findByUser(AppUser user);

    List<UserPermission> findByUserId(int userId);

    Optional<UserPermission> findByUserAndPermission(AppUser user, Permission permission);

    boolean existsByUserAndPermission(AppUser user, Permission permission);

    void deleteByUserAndPermission(AppUser user, Permission permission);
}
