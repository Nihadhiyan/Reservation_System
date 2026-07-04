package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.building.mapper.BuildingMapper;
import com.bookfair.backend.dto.building.response.BuildingResponse;
import com.bookfair.backend.dto.venue.mapper.VenueMapper;
import com.bookfair.backend.dto.venue.request.CreateVenueRequest;
import com.bookfair.backend.dto.venue.request.UpdateVenueRequest;
import com.bookfair.backend.dto.venue.response.VenueMapResponse;
import com.bookfair.backend.dto.venue.response.VenueResponse;
import com.bookfair.backend.event.cache.VenueUpdatedEvent;
import com.bookfair.backend.event.hierarchy.VenueDeactivatedEvent;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.DuplicateResourceException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Building;
import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.EventStall.AvailabilityStatus;
import com.bookfair.backend.model.Floor;
import com.bookfair.backend.model.Hall;
import com.bookfair.backend.model.Organization;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.model.Venue;
import com.bookfair.backend.repository.BuildingRepository;
import com.bookfair.backend.repository.EventStallRepository;
import com.bookfair.backend.repository.FloorRepository;
import com.bookfair.backend.repository.HallRepository;
import com.bookfair.backend.repository.OrganizationRepository;
import com.bookfair.backend.repository.StallRepository;
import com.bookfair.backend.repository.VenueRepository;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VenueService {
        private final VenueRepository venueRepository;
        private final OrganizationRepository organizationRepository;
        private final BuildingRepository buildingRepository;
        private final FloorRepository floorRepository;
        private final HallRepository hallRepository;
        private final StallRepository stallRepository;
        private final EventStallRepository eventStallRepository;
        private final VenueMapper venueMapper;
        private final BuildingMapper buildingMapper;
        private final ApplicationEventPublisher eventPublisher;

        @Transactional
        public VenueResponse createVenue(CreateVenueRequest request) {
                requireNonNull(request, "request cannot be null");
                if (venueRepository.existsByNameAndActiveTrue(requireNonNull(request.getName()))) {
                        throw new DuplicateResourceException(
                                        "A venue with this name already exists.",
                                        ErrorCode.BUSINESS_RULE_VIOLATION);
                }

                Organization owner = organizationRepository.findById(requireNonNull(request.getOwnerOrganizationId()))
                                .orElseThrow(
                                                () -> new ResourceNotFoundException("Owner org not found",
                                                                ErrorCode.ORGANIZATION_NOT_FOUND));

                List<Organization> partners = (request.getPartnerOrganizationIds() != null
                                && !request.getPartnerOrganizationIds().isEmpty())
                                                ? organizationRepository
                                                                .findAllById(requireNonNull(request.getPartnerOrganizationIds()))
                                                : List.of();

                Venue venue = venueMapper.toVenueFromCreateVenueRequest(request);
                venue.setActive(true);
                venue.setOwner(owner);
                venue.setPartners(partners);

                Venue saved = venueRepository.save(venue);
                eventPublisher.publishEvent(new VenueUpdatedEvent(saved.getId()));
                return venueMapper.toVenueResponse(saved);
        }

        @Transactional(readOnly = true)
        public Page<VenueResponse> getAllVenues(Pageable pageable) {
                return venueRepository.findAll(requireNonNull(pageable))
                                .map(venueMapper::toVenueResponse);
        }

        @Cacheable(value = "venues")
        @Transactional(readOnly = true)
        public List<VenueResponse> getAllVenues() {
                return venueRepository.findAll().stream()
                                .filter(v -> Boolean.TRUE.equals(v.getActive()))
                                .map(venueMapper::toVenueResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public VenueResponse getVenue(UUID id) {
                Venue venue = venueRepository.findById(requireNonNull(id))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Venue not found with ID: " + id,
                                                ErrorCode.VENUE_NOT_FOUND));
                return venueMapper.toVenueResponse(venue);
        }

        @Cacheable(value = "venueMap", key = "#id")
        @Transactional(readOnly = true)
        public VenueMapResponse getVenueMap(UUID id) {
                Venue venue = venueRepository.findDetailedById(requireNonNull(id))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Venue not found with ID: " + id,
                                                ErrorCode.VENUE_NOT_FOUND));
                return venueMapper.toVenueMapResponse(venue);
        }

        @Transactional
        public VenueResponse updateVenue(UUID id, UpdateVenueRequest request) {
                Venue venue = venueRepository.findById(requireNonNull(id))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Venue not found with ID: " + id,
                                                ErrorCode.VENUE_NOT_FOUND));

                if (!venue.getName().equals(request.getName()) &&
                                venueRepository.existsByNameAndActiveTrue(requireNonNull(request.getName()))) {
                        throw new DuplicateResourceException(
                                        "A venue with this name already exists.",
                                        ErrorCode.BUSINESS_RULE_VIOLATION);
                }

                boolean oldActive = Boolean.TRUE.equals(venue.getActive());
                if (request.getActive() != null && !request.getActive() && oldActive) {
                        validateNoActiveBookingsForVenue(venue.getId(), venue.getName());
                }

                Organization owner = organizationRepository.findById(requireNonNull(request.getOwnerOrganizationId()))
                                .orElseThrow(
                                                () -> new ResourceNotFoundException("Owner org not found",
                                                                ErrorCode.ORGANIZATION_NOT_FOUND));

                List<Organization> partners = (request.getPartnerOrganizationIds() != null
                                && !request.getPartnerOrganizationIds().isEmpty())
                                                ? organizationRepository
                                                                .findAllById(requireNonNull(request.getPartnerOrganizationIds()))
                                                : List.of();

                venue = venueMapper.updateVenueFromUpdateVenueRequest(request, venue);
                venue.setOwner(owner);
                venue.setPartners(partners);

                Venue saved = venueRepository.save(venue);
                eventPublisher.publishEvent(new VenueUpdatedEvent(saved.getId()));
                if (oldActive && !Boolean.TRUE.equals(saved.getActive())) {
                        eventPublisher.publishEvent(new VenueDeactivatedEvent(saved.getId()));
                }
                return venueMapper.toVenueResponse(saved);
        }

        @Transactional
        public void deleteVenue(UUID id) {
                Venue venue = venueRepository.findById(requireNonNull(id))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Venue not found with ID: " + id,
                                                ErrorCode.VENUE_NOT_FOUND));
                validateNoActiveBookingsForVenue(venue.getId(), venue.getName());
                venue.setActive(false);
                venueRepository.save(venue);
                eventPublisher.publishEvent(new VenueUpdatedEvent(venue.getId()));
                eventPublisher.publishEvent(new VenueDeactivatedEvent(venue.getId()));
        }

        private void validateNoActiveBookingsForVenue(UUID venueId, String venueName) {
                List<Building> buildings = buildingRepository.findByVenueIdAndActiveTrue(venueId);
                for (Building building : buildings) {
                        List<Floor> floors = floorRepository.findByBuildingIdOrderByLevelNumberAsc(building.getId());
                        for (Floor floor : floors) {
                                List<Hall> halls = hallRepository.findByFloorIdAndActiveTrue(floor.getId());
                                for (Hall hall : halls) {
                                        List<Stall> stalls = stallRepository.findByHallIdAndActiveTrue(hall.getId());
                                        for (Stall stall : stalls) {
                                                List<EventStall> esList = eventStallRepository.findByStallIdAndActiveTrue(stall.getId());
                                                for (EventStall es : esList) {
                                                        if (es.getStatus() == AvailabilityStatus.BOOKED || es.getStatus() == AvailabilityStatus.BLOCKED) {
                                                                throw new BusinessException("Cannot deactivate Venue " + venueName + " because stall " + stall.getName() + " is currently booked or blocked in an event.", ErrorCode.BUSINESS_RULE_VIOLATION);
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }

        @Transactional(readOnly = true)
        public List<BuildingResponse> getBuildingsByVenue(UUID venueId) {
                Venue venue = venueRepository.findById(requireNonNull(venueId))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Venue not found with ID: " + venueId,
                                                ErrorCode.VENUE_NOT_FOUND));
                return venue.getBuildings().stream()
                                .map(buildingMapper::toBuildingResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public Object getMarkersByVenue(UUID venueId) {
                Venue venue = venueRepository.findById(requireNonNull(venueId))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Venue not found with ID: " + venueId,
                                                ErrorCode.VENUE_NOT_FOUND));
                return venue.getMarkers();
        }
}
