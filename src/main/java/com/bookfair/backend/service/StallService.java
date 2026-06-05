package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.venue.mapper.VenueMapper;
import com.bookfair.backend.dto.venue.request.CreateStallRequest;
import com.bookfair.backend.dto.venue.request.UpdateStallRequest;
import com.bookfair.backend.dto.venue.response.StallResponse;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.StallRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StallService {

    private final StallRepository stallRepository;
    private final VenueMapper venueMapper;

    @Transactional
    public List<StallResponse> createStall(List<CreateStallRequest> stallRequests) {
        List<Stall> savedStalls = stallRequests.stream().map(stallRequest -> {
            return stallRepository.save(venueMapper.toStallFromCreateStallRequest(stallRequest));
            }
        ).toList();

        return savedStalls.stream().map( savedStall -> {
                return venueMapper.toStallResponse(savedStall);
            }
        ).toList();
    }

    public StallResponse getStallById(UUID id) {
        Stall stall = stallRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Physical Stall not found"));
        
        return venueMapper.toStallResponse(stall);
    }

    @Transactional
    public StallResponse updateStall(UUID id, UpdateStallRequest stallRequest) {
        Stall stall = stallRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Physical Stall not found"));

        venueMapper.updateStallFromStallRequest(stallRequest, stall);

        Stall updatedStall = stallRepository.save(stall);

        return venueMapper.toStallResponse(updatedStall);
    }

    public List<StallResponse> getAvailableStalls() {
        return stallRepository.findAllByActiveTrue().stream()
                .map(stall -> {
                    return venueMapper.toStallResponse(stall);
                }).toList();
    }

    @Transactional
    public void deactivateStall(List<UUID> id) {

        List<Stall> stalls = stallRepository.findAllByIdAndActiveTrue(id);

        for(Stall stall : stalls) {
            stall.setActive(false);
        }

        stallRepository.saveAll(stalls);
    }
    
}
