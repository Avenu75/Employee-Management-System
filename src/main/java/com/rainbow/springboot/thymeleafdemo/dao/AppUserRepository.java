package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {

    @EntityGraph(attributePaths = {"employee", "employee.department"})
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    /** Every enabled account: the audience of announcements and the recipient list of messages. */
    List<AppUser> findByEnabledTrueOrderByFullNameAsc();

    /** Used at startup by the seeder to check "does a super admin already exist?" */
    boolean existsBySuperAdminTrue();

    List<AppUser> findAllByOrderByFullNameAsc();

    /** Every user EXCEPT the super admin - this is the list the admin dashboard's
     *  "Grant Privilege" table shows, since the super admin's own access is not
     *  something that can be edited via the permission-granting UI at all. */
    List<AppUser> findBySuperAdminFalseOrderByFullNameAsc();

    /** The login account linked to an employee record, if any. */
    Optional<AppUser> findFirstByEmployee(Employee employee);

    /** Enabled users who hold the permission, plus every enabled super admin (used for notifications). */
    @Query("select distinct u from AppUser u left join u.grantedPermissions gp left join gp.permission p "
            + "where u.enabled = true and (u.superAdmin = true or p.code = :code)")
    List<AppUser> findNotifiable(@Param("code") String code);
}
