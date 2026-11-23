package com.rainbow.springboot.thymeleafdemo.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.PrivateMessage;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Integer> {

    @Query("select m from PrivateMessage m join fetch m.sender join fetch m.recipient "
            + "where m.sender = :me or m.recipient = :me order by m.createdAt desc")
    List<PrivateMessage> findInvolving(@Param("me") AppUser me, Pageable pageable);

    @Query("select m from PrivateMessage m join fetch m.sender join fetch m.recipient "
            + "where (m.sender = :a and m.recipient = :b) or (m.sender = :b and m.recipient = :a) "
            + "order by m.createdAt asc")
    List<PrivateMessage> findThread(@Param("a") AppUser a, @Param("b") AppUser b);

    long countByRecipientAndReadAtIsNull(AppUser recipient);

    @Modifying
    @Query("update PrivateMessage m set m.readAt = :now where m.recipient = :me and m.sender = :other and m.readAt is null")
    int markRead(@Param("me") AppUser me, @Param("other") AppUser other, @Param("now") LocalDateTime now);
}
