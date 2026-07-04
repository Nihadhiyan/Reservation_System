package com.bookfair.backend.listener;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.bookfair.backend.event.hierarchy.BuildingDeactivatedEvent;
import com.bookfair.backend.event.hierarchy.FloorDeactivatedEvent;
import com.bookfair.backend.event.hierarchy.HallDeactivatedEvent;
import com.bookfair.backend.event.hierarchy.VenueDeactivatedEvent;
import com.bookfair.backend.model.Building;
import com.bookfair.backend.model.Floor;
import com.bookfair.backend.model.Hall;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.repository.BuildingRepository;
import com.bookfair.backend.repository.FloorRepository;
import com.bookfair.backend.repository.HallRepository;
import com.bookfair.backend.repository.StallRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class HierarchyDeactivationListener {

    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final HallRepository hallRepository;
    private final StallRepository stallRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onVenueDeactivated(VenueDeactivatedEvent event) {
        log.info("Processing cascade deactivation for venue: {}", event.venueId());
        List<Building> buildings = buildingRepository.findByVenueIdAndActiveTrue(event.venueId());
        if (!buildings.isEmpty()) {
            buildings.forEach(building -> building.setActive(false));
            buildingRepository.saveAll(buildings);
            log.info("Deactivated {} buildings for venue {}", buildings.size(), event.venueId());
            buildings.forEach(building -> eventPublisher.publishEvent(new BuildingDeactivatedEvent(building.getId())));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onBuildingDeactivated(BuildingDeactivatedEvent event) {
        log.info("Processing cascade deactivation for building: {}", event.buildingId());
        List<Floor> floors = floorRepository.findByBuildingIdAndActiveTrue(event.buildingId());
        if (!floors.isEmpty()) {
            floors.forEach(floor -> floor.setActive(false));
            floorRepository.saveAll(floors);
            log.info("Deactivated {} floors for building {}", floors.size(), event.buildingId());
            floors.forEach(floor -> eventPublisher.publishEvent(new FloorDeactivatedEvent(floor.getId())));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onFloorDeactivated(FloorDeactivatedEvent event) {
        log.info("Processing cascade deactivation for floor: {}", event.floorId());
        List<Hall> halls = hallRepository.findByFloorIdAndActiveTrue(event.floorId());
        if (!halls.isEmpty()) {
            halls.forEach(hall -> hall.setActive(false));
            hallRepository.saveAll(halls);
            log.info("Deactivated {} halls for floor {}", halls.size(), event.floorId());
            halls.forEach(hall -> eventPublisher.publishEvent(new HallDeactivatedEvent(hall.getId())));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onHallDeactivated(HallDeactivatedEvent event) {
        log.info("Processing cascade deactivation for hall: {}", event.hallId());
        List<Stall> stalls = stallRepository.findByHallIdAndActiveTrue(event.hallId());
        if (!stalls.isEmpty()) {
            stalls.forEach(stall -> stall.setActive(false));
            stallRepository.saveAll(stalls);
            log.info("Deactivated {} stalls for hall {}", stalls.size(), event.hallId());
        }
    }
}
