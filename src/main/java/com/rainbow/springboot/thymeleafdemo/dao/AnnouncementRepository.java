package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import com.rainbow.springboot.thymeleafdemo.entity.Announcement;
import com.rainbow.springboot.thymeleafdemo.entity.AnnouncementCategory;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {

    @EntityGraph(attributePaths = {"author"})
    List<Announcement> findByCategoryOrderByCreatedAtDesc(AnnouncementCategory category, Pageable pageable);
}
