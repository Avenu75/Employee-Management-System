package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rainbow.springboot.thymeleafdemo.entity.PayrollItem;

public interface PayrollItemRepository extends JpaRepository<PayrollItem, Integer> {

    @EntityGraph(attributePaths = {"employees"})
    List<PayrollItem> findAllByOrderByKindAscNameAsc();

    @EntityGraph(attributePaths = {"employees"})
    List<PayrollItem> findByActiveTrueOrderByKindAscNameAsc();

    boolean existsByActiveTrue();
}
