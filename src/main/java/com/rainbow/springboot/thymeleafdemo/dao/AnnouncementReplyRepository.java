package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rainbow.springboot.thymeleafdemo.entity.AnnouncementReply;

public interface AnnouncementReplyRepository extends JpaRepository<AnnouncementReply, Integer> {

    @Query("select r from AnnouncementReply r join fetch r.author where r.announcement.id in :ids order by r.createdAt asc")
    List<AnnouncementReply> findForAnnouncements(@Param("ids") Collection<Integer> ids);

    void deleteByAnnouncementId(int announcementId);
}
