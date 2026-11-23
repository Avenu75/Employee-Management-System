package com.rainbow.springboot.thymeleafdemo.service;

import java.util.List;

import com.rainbow.springboot.thymeleafdemo.entity.OtherWorker;

public interface OtherWorkerService {
    List<OtherWorker> findAll();
    OtherWorker findById(int id);
    void save(OtherWorker otherWorker);
    void deleteById(int id);
}
