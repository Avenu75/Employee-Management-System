package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findTop50ByUserOrderByCreatedAtDesc(AppUser user);

    long countByUserAndSeenFalse(AppUser user);

    Optional<Notification> findByIdAndUser(int id, AppUser user);

    @Transactional
    @Modifying
    @Query("update Notification n set n.seen = true where n.user = :user and n.seen = false")
    int markAllSeen(@Param("user") AppUser user);
}
