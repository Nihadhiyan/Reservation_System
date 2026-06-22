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

    EventStallResponse toEventStallResponse(EventStall eventStall);

    EventSummaryResponse toEventSummaryResponse(Event event);
}
