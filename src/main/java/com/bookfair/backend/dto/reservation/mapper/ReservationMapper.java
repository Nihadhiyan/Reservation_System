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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "reservedStalls", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "organization", source = "organization")
    @Mapping(target = "reservationCreatedBy", source = "reservationCreatedBy")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "genre", source = "genre")
    @Mapping(target = "reservationStartDateTime", source = "reservationStartDateTime")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Reservation toReservation(User user, com.bookfair.backend.model.Organization organization, User reservationCreatedBy, Event event, com.bookfair.backend.model.Genre genre, java.time.Instant reservationStartDateTime, java.time.Instant expiresAt);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventStall", source = "eventStall")
    @Mapping(target = "reservation", source = "reservation")
    @Mapping(target = "priceAtBooking", source = "priceAtBooking")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ReservationStall toReservationStall(com.bookfair.backend.model.EventStall eventStall, Reservation reservation, java.math.BigDecimal priceAtBooking);
}

