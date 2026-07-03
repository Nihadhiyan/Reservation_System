package com.bookfair.backend.dto.event.mapper;

import org.mapstruct.Mapper;

import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.event.response.EventResponse;
import com.bookfair.backend.dto.event.response.EventStallResponse;
import com.bookfair.backend.dto.event.response.EventSummaryResponse;
import com.bookfair.backend.dto.organization.mapper.OrganizationMapper;
import com.bookfair.backend.model.Event;
import com.bookfair.backend.model.EventStall;

@Mapper(
    config = GlobalMapperConfig.class,
    uses = {OrganizationMapper.class}
)
public interface EventMapper {
    EventResponse toEventResponse(Event event);

    @org.mapstruct.Mapping(source = "stallNameAtCreation", target = "stallName")
    @org.mapstruct.Mapping(source = "hallNameAtCreation", target = "hallName")
    EventStallResponse toEventStallResponse(EventStall eventStall);

    EventSummaryResponse toEventSummaryResponse(Event event);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "organizer", source = "organizer")
    @org.mapstruct.Mapping(target = "venue", source = "venue")
    @org.mapstruct.Mapping(target = "partners", source = "partners")
    @org.mapstruct.Mapping(target = "name", source = "request.name")
    @org.mapstruct.Mapping(target = "eventType", source = "request.eventType")
    @org.mapstruct.Mapping(target = "startDateTime", source = "request.startDateTime")
    @org.mapstruct.Mapping(target = "endDateTime", source = "request.endDateTime")
    @org.mapstruct.Mapping(target = "status", source = "request.status")
    @org.mapstruct.Mapping(target = "createdBy", ignore = true)
    @org.mapstruct.Mapping(target = "updatedBy", ignore = true)
    @org.mapstruct.Mapping(target = "version", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    @org.mapstruct.Mapping(target = "active", ignore = true)
    Event toEvent(com.bookfair.backend.dto.event.request.CreateEventRequest request, com.bookfair.backend.model.Organization organizer, com.bookfair.backend.model.Venue venue, java.util.List<com.bookfair.backend.model.Organization> partners);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "event", source = "event")
    @org.mapstruct.Mapping(target = "stall", source = "stall")
    @org.mapstruct.Mapping(target = "stallNameAtCreation", source = "stall.name")
    @org.mapstruct.Mapping(target = "hallIdSnapshot", source = "stall.hall.id")
    @org.mapstruct.Mapping(target = "hallNameAtCreation", source = "stall.hall.name")
    @org.mapstruct.Mapping(target = "layout", source = "stall.layout")
    @org.mapstruct.Mapping(target = "basePrice", source = "request.basePrice")
    @org.mapstruct.Mapping(target = "manualOverridePrice", source = "request.manualOverridePrice")
    @org.mapstruct.Mapping(target = "status", expression = "java(com.bookfair.backend.model.EventStall.AvailabilityStatus.valueOf(request.getStatus().toUpperCase()))")
    @org.mapstruct.Mapping(target = "createdBy", ignore = true)
    @org.mapstruct.Mapping(target = "updatedBy", ignore = true)
    @org.mapstruct.Mapping(target = "version", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    @org.mapstruct.Mapping(target = "active", ignore = true)
    EventStall toEventStall(com.bookfair.backend.dto.event.request.CreateEventStallRequest request, Event event, com.bookfair.backend.model.Stall stall);

    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "event", source = "event")
    @org.mapstruct.Mapping(target = "stall", source = "stall")
    @org.mapstruct.Mapping(target = "stallNameAtCreation", source = "stall.name")
    @org.mapstruct.Mapping(target = "hallIdSnapshot", source = "stall.hall.id")
    @org.mapstruct.Mapping(target = "hallNameAtCreation", source = "stall.hall.name")
    @org.mapstruct.Mapping(target = "layout", source = "stall.layout")
    @org.mapstruct.Mapping(target = "basePrice", expression = "java(java.math.BigDecimal.ZERO)")
    @org.mapstruct.Mapping(target = "manualOverridePrice", ignore = true)
    @org.mapstruct.Mapping(target = "status", constant = "AVAILABLE")
    @org.mapstruct.Mapping(target = "createdBy", ignore = true)
    @org.mapstruct.Mapping(target = "updatedBy", ignore = true)
    @org.mapstruct.Mapping(target = "version", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    @org.mapstruct.Mapping(target = "active", ignore = true)
    EventStall toCopiedEventStall(Event event, com.bookfair.backend.model.Stall stall);
}
