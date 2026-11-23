package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rainbow.springboot.thymeleafdemo.entity.PayrollAdjustment;

public interface PayrollAdjustmentRepository extends JpaRepository<PayrollAdjustment, Integer> {

    @EntityGraph(attributePaths = {"employee"})
    List<PayrollAdjustment> findByMonthOrderByIdAsc(String month);
}
