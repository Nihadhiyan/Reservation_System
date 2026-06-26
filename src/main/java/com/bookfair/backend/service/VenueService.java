package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;

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
import com.bookfair.backend.exception.DuplicateResourceException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Organization;
import com.bookfair.backend.model.Venue;
import com.bookfair.backend.repository.OrganizationRepository;
import com.bookfair.backend.repository.VenueRepository;

import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VenueService {
        private final VenueRepository venueRepository;
        private final OrganizationRepository organizationRepository;
        private final VenueMapper venueMapper;
        private final BuildingMapper buildingMapper;

        @Transactional
        public VenueResponse createVenue(CreateVenueRequest request) {
                requireNonNull(request, "request cannot be null");
                if (venueRepository.existsByNameAndActiveTrue(request.getName())) {
                        throw new DuplicateResourceException(
                                        "A venue with this name already exists.",
                                        ErrorCode.BUSINESS_RULE_VIOLATION);
                }

                Organization owner = organizationRepository.findById(request.getOwnerOrganizationId())
                                .orElseThrow(
                                                () -> new ResourceNotFoundException("Owner org not found",
                                                                ErrorCode.ORGANIZATION_NOT_FOUND));

                List<Organization> partners = (request.getPartnerOrganizationIds() != null
                                && !request.getPartnerOrganizationIds().isEmpty())
                                                ? organizationRepository
                                                                .findAllById(request.getPartnerOrganizationIds())
                                                : List.of();

                Venue venue = venueMapper.toVenueFromCreateVenueRequest(request);
                venue.setActive(true);
                venue.setOwner(owner);
                venue.setPartners(partners);

                return venueMapper.toVenueResponse(venueRepository.save(venue));
        }

        @Transactional(readOnly = true)
        public Page<VenueResponse> getAllVenues(Pageable pageable) {
                return venueRepository.findAll(pageable)
                                .map(venueMapper::toVenueResponse);
        }

        @Transactional(readOnly = true)
        public List<VenueResponse> getAllVenues() {
                return venueRepository.findAll().stream()
                                .filter(v -> Boolean.TRUE.equals(v.getActive()))
                                .map(venueMapper::toVenueResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public VenueResponse getVenue(UUID id) {
                Venue venue = venueRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Venue not found with ID: " + id,
                                                ErrorCode.VENUE_NOT_FOUND));
                return venueMapper.toVenueResponse(venue);
        }

        @Transactional(readOnly = true)
        public VenueMapResponse getVenueMap(UUID id) {
                Venue venue = venueRepository.findDetailedById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Venue not found with ID: " + id,
                                                ErrorCode.VENUE_NOT_FOUND));
                return venueMapper.toVenueMapResponse(venue);
        }

        @Transactional
        public VenueResponse updateVenue(UUID id, UpdateVenueRequest request) {
                Venue venue = venueRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Venue not found with ID: " + id,
                                                ErrorCode.VENUE_NOT_FOUND));

                if (!venue.getName().equals(request.getName()) &&
                                venueRepository.existsByNameAndActiveTrue(request.getName())) {
                        throw new DuplicateResourceException(
                                        "A venue with this name already exists.",
                                        ErrorCode.BUSINESS_RULE_VIOLATION);
                }

                Organization owner = organizationRepository.findById(request.getOwnerOrganizationId())
                                .orElseThrow(
                                                () -> new ResourceNotFoundException("Owner org not found",
                                                                ErrorCode.ORGANIZATION_NOT_FOUND));

                List<Organization> partners = (request.getPartnerOrganizationIds() != null
                                && !request.getPartnerOrganizationIds().isEmpty())
                                                ? organizationRepository
                                                                .findAllById(request.getPartnerOrganizationIds())
                                                : List.of();

                venue = venueMapper.updateVenueFromUpdateVenueRequest(request, venue);
                venue.setOwner(owner);
                venue.setPartners(partners);

                return venueMapper.toVenueResponse(venueRepository.save(venue));
        }

        @Transactional
        public void deleteVenue(UUID id) {
                Venue venue = venueRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Venue not found with ID: " + id,
                                                ErrorCode.VENUE_NOT_FOUND));
                venue.setActive(false);
                venueRepository.save(venue);
        }

        @Transactional(readOnly = true)
        public List<BuildingResponse> getBuildingsByVenue(UUID venueId) {
                Venue venue = venueRepository.findById(venueId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Venue not found with ID: " + venueId,
                                                ErrorCode.VENUE_NOT_FOUND));
                return venue.getBuildings().stream()
                                .map(buildingMapper::toBuildingResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public Object getMarkersByVenue(UUID venueId) {
                Venue venue = venueRepository.findById(venueId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Venue not found with ID: " + venueId,
                                                ErrorCode.VENUE_NOT_FOUND));
                return venue.getMarkers();
        }
}
