package com.bookfair.backend.service;

import java.util.List;
import java.util.UUID;

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
import com.bookfair.backend.model.Organization;
import com.bookfair.backend.model.User;
import com.bookfair.backend.model.SystemRole;
import com.bookfair.backend.model.OrganizationRole;
import com.bookfair.backend.model.OrganizationMember;
import com.bookfair.backend.model.Venue;
import com.bookfair.backend.repository.EventRepository;
import com.bookfair.backend.repository.EventStallRepository;
import com.bookfair.backend.repository.OrganizationRepository;
import com.bookfair.backend.repository.UserRepository;
import com.bookfair.backend.repository.OrganizationMemberRepository;
import com.bookfair.backend.repository.VenueRepository;
import com.bookfair.backend.security.CustomUserPrincipal;
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

    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents() {
        return eventRepository.findByStatusAndActiveTrue(EventStatus.UPCOMING).stream()
                .map(eventMapper::toEventResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getAllEvents(Pageable pageable) {
        requireNonNull(pageable, "pageable cannot be null");
        return eventRepository.findAll(pageable)
                .map(eventMapper::toEventResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID id) {
        Event event = eventRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND));
        return eventMapper.toEventResponse(event);
    }

    @Transactional(readOnly = true)
    public List<EventStallResponse> getStallsForEvent(UUID eventId) {
        Event event = eventRepository.findByIdAndActiveTrue(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND));

        return eventStallRepository.findByEvent(event).stream()
                .map(eventMapper::toEventStallResponse)
                .toList();
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        Organization organizer = organizationRepository.findById(request.getOrganizerId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                        ErrorCode.ORGANIZATION_NOT_FOUND));

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found", ErrorCode.VENUE_NOT_FOUND));

        User requestingUser = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        if (requestingUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
            OrganizationMember member = memberRepository.findByUserIdAndOrganizationId(requestingUser.getId(), organizer.getId())
                    .orElse(null);
            if (member == null || member.getRole() != OrganizationRole.ORG_ADMIN) {
                throw new ForbiddenException("You cannot create an event for another organization.",
                        ErrorCode.FORBIDDEN);
            }
        }

        List<Organization> partners = (request.getPartnerIds() != null && !request.getPartnerIds().isEmpty())
                ? organizationRepository.findAllById(request.getPartnerIds())
                : List.of();

        Event event = new Event();
        event.setName(request.getName());
        event.setEventType(request.getEventType());
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setStatus(request.getStatus());
        event.setOrganizer(organizer);
        event.setVenue(venue);
        event.setPartners(partners);
        event.setActive(true);

        return eventMapper.toEventResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse updateEvent(UUID id, UpdateEventRequest request) {
        Event event = eventRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND));

        Organization organizer = organizationRepository.findById(request.getOrganizerId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found",
                        ErrorCode.ORGANIZATION_NOT_FOUND));

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found", ErrorCode.VENUE_NOT_FOUND));

        User requestingUser = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        if (requestingUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
            OrganizationMember member = memberRepository.findByUserIdAndOrganizationId(requestingUser.getId(), event.getOrganizer().getId())
                    .orElse(null);
            if (member == null || member.getRole() != OrganizationRole.ORG_ADMIN) {
                throw new ForbiddenException("You cannot modify an event outside your organization.",
                        ErrorCode.FORBIDDEN);
            }
            if (!event.getOrganizer().getId().equals(organizer.getId())) {
                OrganizationMember newOrgMember = memberRepository.findByUserIdAndOrganizationId(requestingUser.getId(), organizer.getId())
                        .orElse(null);
                if (newOrgMember == null || newOrgMember.getRole() != OrganizationRole.ORG_ADMIN) {
                    throw new ForbiddenException("You cannot transfer an event to another organization.",
                            ErrorCode.FORBIDDEN);
                }
            }
        }

        List<Organization> partners = (request.getPartnerIds() != null && !request.getPartnerIds().isEmpty())
                ? organizationRepository.findAllById(request.getPartnerIds())
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

        return eventMapper.toEventResponse(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(UUID id) {
        Event event = eventRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND));

        User requestingUser = userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found", ErrorCode.USER_NOT_FOUND));

        if (requestingUser.getSystemRole() != SystemRole.SUPER_ADMIN) {
            OrganizationMember member = memberRepository.findByUserIdAndOrganizationId(requestingUser.getId(), event.getOrganizer().getId())
                    .orElse(null);
            if (member == null || member.getRole() != OrganizationRole.ORG_ADMIN) {
                throw new ForbiddenException("You cannot delete an event outside your organization.",
                        ErrorCode.FORBIDDEN);
            }
        }

        event.setActive(false);
        eventRepository.save(event);
    }

    @Transactional
    public void changeStatus(UUID eventId, String newStatusString) {
        Event eventInstance = eventRepository.findByIdAndActiveTrue(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND));

        String oldStatus = eventInstance.getStatus().name();
        Event.EventStatus newStatus = Event.EventStatus.valueOf(newStatusString.toUpperCase());

        if (!oldStatus.equals(newStatus.name())) {
            eventInstance.setStatus(newStatus);
            eventRepository.save(eventInstance);

            eventPublisher.publishEvent(new com.bookfair.backend.event.event.EventStatusChangedEvent(
                    eventInstance.getId(),
                    oldStatus,
                    newStatus.name()));

        }
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            return principal.getId();
        }

        throw new BusinessException("Unable to resolve current user", ErrorCode.UNAUTHORIZED);
    }
}
