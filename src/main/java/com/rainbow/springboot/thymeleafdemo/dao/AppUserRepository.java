package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rainbow.springboot.thymeleafdemo.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    /** Used at startup by the seeder to check "does a super admin already exist?" */
    boolean existsBySuperAdminTrue();

    List<AppUser> findAllByOrderByFullNameAsc();

    /** Every user EXCEPT the super admin - this is the list the admin dashboard's
     *  "Grant Privilege" table shows, since the super admin's own access is not
     *  something that can be edited via the permission-granting UI at all. */
    List<AppUser> findBySuperAdminFalseOrderByFullNameAsc();
}
