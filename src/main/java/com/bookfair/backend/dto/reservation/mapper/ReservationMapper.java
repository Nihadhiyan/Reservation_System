package com.bookfair.backend.dto.reservation.mapper;

import org.mapstruct.Mapper;

import com.bookfair.backend.dto.common.SimpleEventDto;
import com.bookfair.backend.dto.common.SimpleStallDto;
import com.bookfair.backend.dto.common.SimpleUserDto;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.reservation.response.ReservationDetailResponse;
import com.bookfair.backend.dto.reservation.response.ReservationResponse;
import com.bookfair.backend.dto.reservation.response.ReservationStallResponse;
import com.bookfair.backend.dto.reservation.response.ReservationSummaryResponse;
import com.bookfair.backend.model.Event;
import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.model.ReservationStall;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.model.User;

import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface ReservationMapper {

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "reservationCreatedByUserId", source = "reservationCreatedBy.id")
    @Mapping(target = "reservationCreatedByUsername", source = "reservationCreatedBy.username")
    ReservationResponse toReservationResponse(Reservation reservation);

    ReservationSummaryResponse toReservationSummaryResponse(Reservation reservation);

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "reservationCreatedByUserId", source = "reservationCreatedBy.id")
    @Mapping(target = "reservationCreatedByUsername", source = "reservationCreatedBy.username")
    ReservationDetailResponse toReservationDetailResponse(Reservation reservation);

    ReservationStallResponse toReservationStallResponse(ReservationStall reservationStall);

    SimpleUserDto toSimpleUserDto(User user);

    SimpleEventDto toSimpleEventDto(Event event);

    SimpleStallDto toSimpleStallDto(Stall stall);
}
