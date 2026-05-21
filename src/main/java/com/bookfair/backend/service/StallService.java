package com.bookfair.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bookfair.backend.dto.request.StallRequest;
import com.bookfair.backend.dto.response.StallResponse;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.model.Stall.StallStatus;
import com.bookfair.backend.repository.StallRepository;

@Service
public class StallService {

    private final StallRepository stallRepository;

    public StallService(StallRepository stallRepository) {
        this.stallRepository = stallRepository;
    }

    public List<StallResponse> getAllStalls() {
        return stallRepository.findAll().stream()
        .map(stall -> {
            StallResponse stallResponse = new StallResponse();
            stallResponse.setId(stall.getId());
            stallResponse.setName(stall.getName());
            stallResponse.setSize(stall.getSize().name());
            stallResponse.setPrice(stall.getPrice());
            stallResponse.setStatus(stall.getStatus().name());
            stallResponse.setXCoord(stall.getXCoord());
            stallResponse.setYCoord(stall.getYCoord());
            return stallResponse;
        }).toList();
    }

    public StallResponse createStall(StallRequest stallRequest) {
        Stall stall = new Stall();
        stall.setName(stallRequest.getName());
        stall.setSize(Stall.StallSize.valueOf(stallRequest.getSize()));
        stall.setPrice(stallRequest.getPrice());
        stall.setStatus(StallStatus.valueOf(stallRequest.getStatus()));
        stall.setXCoord(stallRequest.getXCoord());
        stall.setYCoord(stallRequest.getYCoord());

        stallRepository.save(stall);

        StallResponse stallResponse = new StallResponse();
        stallResponse.setId(stall.getId());
        stallResponse.setName(stall.getName());
        stallResponse.setSize(stall.getSize().name());
        stallResponse.setPrice(stall.getPrice());
        stallResponse.setStatus(stall.getStatus().name());
        stallResponse.setXCoord(stall.getXCoord());
        stallResponse.setYCoord(stall.getYCoord());

       return stallResponse;
    }

    public StallResponse getStallById(Long id) {
        Stall stall = stallRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stall not found with id: " + id));
        
        StallResponse stallResponse = new StallResponse();
        stallResponse.setId(stall.getId());
        stallResponse.setName(stall.getName());
        stallResponse.setSize(stall.getSize().name());
        stallResponse.setPrice(stall.getPrice());
        stallResponse.setStatus(stall.getStatus().name());
        stallResponse.setXCoord(stall.getXCoord());
        stallResponse.setYCoord(stall.getYCoord());
        
        return stallResponse;
    }

    public StallResponse updateStall(Long id, StallRequest stallRequest) {
        Stall stall = stallRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Stall not found with id: " + id));

        stall.setName(stallRequest.getName());
        stall.setSize(Stall.StallSize.valueOf(stallRequest.getSize()));
        stall.setPrice(stallRequest.getPrice());
        stall.setStatus(StallStatus.valueOf(stallRequest.getStatus()));
        stall.setXCoord(stallRequest.getXCoord());
        stall.setYCoord(stallRequest.getYCoord());

        stallRepository.save(stall);

        StallResponse stallResponse = new StallResponse();
        stallResponse.setId(stall.getId());
        stallResponse.setName(stall.getName());
        stallResponse.setSize(stall.getSize().name());
        stallResponse.setPrice(stall.getPrice());
        stallResponse.setStatus(stall.getStatus().name());
        stallResponse.setXCoord(stall.getXCoord());
        stallResponse.setYCoord(stall.getYCoord());

        return stallResponse;
    }

    public List<StallResponse> getAvailableStalls() {
        return stallRepository.findByStatus(StallStatus.AVAILABLE).stream()
                .map(stall -> {
                    StallResponse stallResponse = new StallResponse();
                    stallResponse.setId(stall.getId());
                    stallResponse.setName(stall.getName());
                    stallResponse.setSize(stall.getSize().name());
                    stallResponse.setPrice(stall.getPrice());
                    stallResponse.setStatus(stall.getStatus().name());
                    stallResponse.setXCoord(stall.getXCoord());
                    stallResponse.setYCoord(stall.getYCoord());
                    return stallResponse;
                }).toList();
    }
    
}
