package com.rainbow.springboot.thymeleafdemo.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rainbow.springboot.thymeleafdemo.entity.OtherWorker;
import com.rainbow.springboot.thymeleafdemo.entity.WorkerAttendance;

public interface WorkerAttendanceRepository extends JpaRepository<WorkerAttendance, Integer> {

    Optional<WorkerAttendance> findByOtherWorkerAndAttendanceDate(OtherWorker worker, LocalDate date);

    @Query("select w from WorkerAttendance w join fetch w.otherWorker where w.attendanceDate = :date")
    List<WorkerAttendance> findByAttendanceDate(@Param("date") LocalDate date);
}
