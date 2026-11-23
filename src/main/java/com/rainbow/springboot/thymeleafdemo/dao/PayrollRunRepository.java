package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rainbow.springboot.thymeleafdemo.entity.PayrollRun;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Integer> {

    @EntityGraph(attributePaths = {"lines"})
    Optional<PayrollRun> findByMonth(String month);

    List<PayrollRun> findTop24ByOrderByMonthDesc();
}
