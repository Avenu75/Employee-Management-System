package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rainbow.springboot.thymeleafdemo.entity.OtherWorker;

public interface OtherWorkerRepository extends JpaRepository<OtherWorker, Integer> {
    @Override
    @EntityGraph(attributePaths = {"department"})
    Optional<OtherWorker> findById(Integer id);

    @EntityGraph(attributePaths = {"department"})
    List<OtherWorker> findAllByOrderByFullNameAsc();
}
