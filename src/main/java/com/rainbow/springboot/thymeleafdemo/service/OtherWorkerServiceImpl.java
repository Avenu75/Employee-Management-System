package com.rainbow.springboot.thymeleafdemo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.OtherWorkerRepository;
import com.rainbow.springboot.thymeleafdemo.entity.OtherWorker;
import com.rainbow.springboot.thymeleafdemo.exception.OtherWorkerNotFoundException;

@Service
public class OtherWorkerServiceImpl implements OtherWorkerService {

    private final OtherWorkerRepository otherWorkerRepository;

    public OtherWorkerServiceImpl(OtherWorkerRepository theOtherWorkerRepository) {
        otherWorkerRepository = theOtherWorkerRepository;
    }

    @Override
    public List<OtherWorker> findAll() {
        return otherWorkerRepository.findAllByOrderByFullNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public OtherWorker findById(int id) {
        Optional<OtherWorker> result = otherWorkerRepository.findById(id);
        if (result.isEmpty()) {
            throw new OtherWorkerNotFoundException("Did not find other worker id - " + id);
        }
        OtherWorker worker = result.get();
        if (worker.getDepartment() != null) {
            worker.getDepartment().getName();
        }
        return worker;
    }

    @Override
    public void save(OtherWorker otherWorker) {
        otherWorkerRepository.save(otherWorker);
    }

    @Override
    public void deleteById(int id) {
        otherWorkerRepository.deleteById(id);
    }
}
