package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import com.bookfair.backend.event.cache.EventUpdatedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.event.mapper.EventMapper;
import com.bookfair.backend.dto.event.request.CreateEventRequest;
import com.bookfair.backend.dto.event.request.UpdateEventRequest;
import com.bookfair.backend.dto.event.response.EventResponse;
import com.bookfair.backend.dto.event.response.EventStallResponse;
import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.exception.ForbiddenException;
import com.bookfair.backend.exception.ResourceNotFoundException;
import com.bookfair.backend.model.Event;
import com.bookfair.backend.model.Event.EventStatus;
import com.bookfair.backend.model.EventStall;
import com.bookfair.backend.model.Organization;
import com.bookfair.backend.model.User;
import com.bookfair.backend.model.User.SystemRole;
import com.bookfair.backend.model.OrganizationMember.OrganizationRole;
import com.bookfair.backend.model.OrganizationMember;
import com.bookfair.backend.model.Venue;
import com.bookfair.backend.repository.EventRepository;
import com.bookfair.backend.repository.EventStallRepository;
import com.bookfair.backend.repository.OrganizationRepository;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.repository.OrganizationMemberRepository;
import com.bookfair.backend.repository.VenueRepository;
import static java.util.Objects.requireNonNull;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {
        private final EventRepository eventRepository;
        private final EventStallRepository eventStallRepository;
        private final OrganizationRepository organizationRepository;
        private final VenueRepository venueRepository;
        private final UserRepository userRepository;
        private final OrganizationMemberRepository memberRepository;
        private final EventMapper eventMapper;
        private final org.springframework.context.ApplicationEventPublisher eventPublisher;

        // Cache upcoming events list to optimize high-traffic landing page requests
        @Cacheable(value = "events", key = "'upcoming'")
        @Transactional(readOnly = true)
        public List<EventResponse> getUpcomingEvents() {
                return eventRepository.findByStatusAndActiveTrue(EventStatus.UPCOMING).stream()
                                .map(eventMapper::toEventResponse)
                                .toList();
        }

        // Cache paginated event catalog queries
        @Cacheable(value = "events", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
        @Transactional(readOnly = true)
        public Page<EventResponse> getAllEvents(Pageable pageable) {
                requireNonNull(pageable, "pageable cannot be null");
                return eventRepository.findAll(pageable)
                                .map(eventMapper::toEventResponse);
        }

        // Cache individual event details by ID
        @Cacheable(value = "events", key = "#id")
        @Transactional(readOnly = true)
        public EventResponse getEventById(UUID id) {
                Event event = eventRepository.findByIdAndActiveTrue(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Event not found",
                                                ErrorCode.EVENT_NOT_FOUND));
                return eventMapper.toEventResponse(event);
        }

        @Transactional(readOnly = true)
        public List<EventStallResponse> getStallsForEvent(UUID eventId) {
                Event event = eventRepository.findByIdAndActiveTrue(requireNonNull(eventId))
                                .orElseThrow(() -> new ResourceNotFoundException("Event not found",
                                                ErrorCode.EVENT_NOT_FOUND));

                return eventStallRepository.findByEvent(event).stream()
                                .map(eventMapper::toEventStallResponse)
                                .toList();
        }

        @Transactional
        public EventResponse createEvent(CreateEventRequest request) {
                requireNonNull(request, "request cannot be null");
                Organization organizer = organizationRepository.findById(requireNonNull(request.getOrganizerId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                                                ErrorCode.ORGANIZATION_NOT_FOUND));

                Venue venue = venueRepository.findById(requireNonNull(request.getVenueId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Venue not found",
                                                ErrorCode.VENUE_NOT_FOUND));

                User requestingUser = userRepository.findById(requireNonNull(getCurrentUserId()))
                                .orElseThrow(() -> new ResourceNotFoundException("User not found",
                                                ErrorCode.USER_NOT_FOUND));

                if (requestingUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
                        OrganizationMember member = memberRepository
                                        .findByUserIdAndOrganizationId(requestingUser.getId(), organizer.getId())
                                        .orElse(null);
                        if (member == null || member.getRole() != OrganizationRole.ORG_ADMIN) {
                                throw new ForbiddenException("You cannot create an event for another organization.",
                                                ErrorCode.FORBIDDEN);
                        }
                }

                List<Organization> partners = (request.getPartnerIds() != null && !request.getPartnerIds().isEmpty())
                                ? organizationRepository.findAllById(requireNonNull(request.getPartnerIds()))
                                : List.of();

                Event event = eventMapper.toEvent(request, organizer, venue, partners);
                Event savedEvent = eventRepository.save(requireNonNull(event));

                // Publish event to trigger AFTER_COMMIT cache eviction
                eventPublisher.publishEvent(new EventUpdatedEvent(savedEvent.getId()));

                return eventMapper.toEventResponse(savedEvent);
        }

        @Transactional
        public EventResponse updateEvent(UUID id, UpdateEventRequest request) {
                requireNonNull(request, "request cannot be null");
                Event event = eventRepository.findByIdAndActiveTrue(requireNonNull(id))
                                .orElseThrow(() -> new ResourceNotFoundException("Event not found",
                                                ErrorCode.EVENT_NOT_FOUND));

                Organization organizer = organizationRepository.findById(requireNonNull(request.getOrganizerId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                                                ErrorCode.ORGANIZATION_NOT_FOUND));

                Venue venue = venueRepository.findById(requireNonNull(request.getVenueId()))
                                .orElseThrow(() -> new ResourceNotFoundException("Venue not found",
                                                ErrorCode.VENUE_NOT_FOUND));

                User requestingUser = userRepository.findById(requireNonNull(getCurrentUserId()))
                                .orElseThrow(() -> new ResourceNotFoundException("User not found",
                                                ErrorCode.USER_NOT_FOUND));

                if (requestingUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
                        OrganizationMember member = memberRepository
                                        .findByUserIdAndOrganizationId(requestingUser.getId(),
                                                        event.getOrganizer().getId())
                                        .orElse(null);
                        if (member == null || member.getRole() != OrganizationRole.ORG_ADMIN) {
                                throw new ForbiddenException("You cannot modify an event outside your organization.",
                                                ErrorCode.FORBIDDEN);
                        }
                        if (!event.getOrganizer().getId().equals(organizer.getId())) {
                                OrganizationMember newOrgMember = memberRepository
                                                .findByUserIdAndOrganizationId(requestingUser.getId(),
                                                                organizer.getId())
                                                .orElse(null);
                                if (newOrgMember == null || newOrgMember.getRole() != OrganizationRole.ORG_ADMIN) {
                                        throw new ForbiddenException(
                                                        "You cannot transfer an event to another organization.",
                                                        ErrorCode.FORBIDDEN);
                                }
                        }
                }

                if (!event.getVenue().getId().equals(venue.getId())) {
                        List<EventStall> esList = eventStallRepository.findByEventIdAndActiveTrue(event.getId());
                        if (!esList.isEmpty()) {
                                throw new BusinessException("Cannot change Event Venue because stalls from the original Venue are currently assigned to this Event.",
                                                ErrorCode.BUSINESS_RULE_VIOLATION);
                        }
                }

                boolean oldActive = Boolean.TRUE.equals(event.getActive());
                if (request.getActive() != null && !request.getActive() && oldActive) {
                        validateNoActiveBookingsForEvent(event.getId(), event.getName());
                }

                List<Organization> partners = (request.getPartnerIds() != null && !request.getPartnerIds().isEmpty())
                                ? organizationRepository.findAllById(requireNonNull(request.getPartnerIds()))
                                : List.of();

                event.setName(request.getName());
                event.setEventType(request.getEventType());
                event.setStartDateTime(request.getStartDateTime());
                event.setEndDateTime(request.getEndDateTime());
                event.setStatus(request.getStatus());
                event.setActive(request.getActive() != null ? request.getActive() : event.getActive());
                event.setOrganizer(organizer);
                event.setVenue(venue);
                event.setPartners(partners);

                Event updatedEvent = eventRepository.save(event);

                // Publish event to trigger AFTER_COMMIT cache eviction
                eventPublisher.publishEvent(new EventUpdatedEvent(updatedEvent.getId()));
                if (oldActive && !Boolean.TRUE.equals(updatedEvent.getActive())) {
                        eventPublisher.publishEvent(new com.bookfair.backend.event.hierarchy.EventDeactivatedEvent(updatedEvent.getId()));
                }

                return eventMapper.toEventResponse(updatedEvent);
        }

        @Transactional
        public void deleteEvent(UUID id) {
                Event event = eventRepository.findByIdAndActiveTrue(requireNonNull(id))
                                .orElseThrow(() -> new ResourceNotFoundException("Event not found",
                                                ErrorCode.EVENT_NOT_FOUND));

                User requestingUser = userRepository.findById(requireNonNull(getCurrentUserId()))
                                .orElseThrow(() -> new ResourceNotFoundException("User not found",
                                                ErrorCode.USER_NOT_FOUND));

                if (requestingUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
                        OrganizationMember member = memberRepository
                                        .findByUserIdAndOrganizationId(requestingUser.getId(),
                                                        event.getOrganizer().getId())
                                        .orElse(null);
                        if (member == null || member.getRole() != OrganizationRole.ORG_ADMIN) {
                                throw new ForbiddenException("You cannot delete an event outside your organization.",
                                                ErrorCode.FORBIDDEN);
                        }
                }

                validateNoActiveBookingsForEvent(event.getId(), event.getName());

                event.setActive(false);
                eventRepository.save(event);

                // Publish event to trigger AFTER_COMMIT cache eviction
                eventPublisher.publishEvent(new EventUpdatedEvent(event.getId()));
                eventPublisher.publishEvent(new com.bookfair.backend.event.hierarchy.EventDeactivatedEvent(event.getId()));
        }

        private void validateNoActiveBookingsForEvent(UUID eventId, String eventName) {
                List<EventStall> esList = eventStallRepository.findByEventIdAndActiveTrue(eventId);
                for (EventStall es : esList) {
                        if (es.getStatus() == EventStall.AvailabilityStatus.BOOKED || es.getStatus() == EventStall.AvailabilityStatus.BLOCKED) {
                                throw new BusinessException("Cannot deactivate Event " + eventName + " because stall " + es.getStall().getName() + " is currently booked or blocked.",
                                                ErrorCode.BUSINESS_RULE_VIOLATION);
                        }
                }
        }

        @Transactional
        public void changeStatus(UUID eventId, String newStatusString) {
                Event eventInstance = eventRepository.findByIdAndActiveTrue(eventId)
                                .orElseThrow(() -> new ResourceNotFoundException("Event not found",
                                                ErrorCode.EVENT_NOT_FOUND));

                String oldStatus = eventInstance.getStatus().name();
                Event.EventStatus newStatus = Event.EventStatus.valueOf(newStatusString.toUpperCase());

                if (!oldStatus.equals(newStatus.name())) {
                        eventInstance.setStatus(newStatus);
                        eventRepository.save(eventInstance);

                        eventPublisher.publishEvent(new com.bookfair.backend.event.event.EventStatusChangedEvent(
                                        eventInstance.getId(),
                                        oldStatus,
                                        newStatus.name()));
                        // Publish event to trigger AFTER_COMMIT cache eviction
                        eventPublisher.publishEvent(new EventUpdatedEvent(eventInstance.getId()));
                }
        }

        private UUID getCurrentUserId() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
                        return userId;
                }

                if (authentication != null && authentication.getPrincipal() instanceof String userIdString) {
                        return UUID.fromString(userIdString);
                }

                throw new BusinessException("Unable to resolve current user", ErrorCode.UNAUTHORIZED);
        }
}
