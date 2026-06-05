package com.bookfair.backend.dto.reservation.mapper;

import org.mapstruct.Mapper;

import com.bookfair.backend.dto.common.SimpleBookFairDto;
import com.bookfair.backend.dto.common.SimpleStallDto;
import com.bookfair.backend.dto.common.SimpleUserDto;
import com.bookfair.backend.dto.config.GlobalMapperConfig;
import com.bookfair.backend.dto.reservation.response.ReservationDetailResponse;
import com.bookfair.backend.dto.reservation.response.ReservationResponse;
import com.bookfair.backend.dto.reservation.response.ReservationStallResponse;
import com.bookfair.backend.dto.reservation.response.ReservationSummaryResponse;
import com.bookfair.backend.model.BookFair;
import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.model.ReservationStall;
import com.bookfair.backend.model.Stall;
import com.bookfair.backend.model.User;

@Mapper(config = GlobalMapperConfig.class)
public interface ReservationMapper {

    ReservationResponse toReservationResponse(Reservation reservation);

    ReservationSummaryResponse toReservationSummaryResponse(Reservation reservation);

    ReservationDetailResponse toReservationDetailResponse(Reservation reservation);

    ReservationStallResponse toReservationStallResponse(ReservationStall reservationStall);

    SimpleUserDto toSimpleUserDto(User user);

    SimpleBookFairDto toSimpleBookFairDto(BookFair bookFair);

    SimpleStallDto toSimpleStallDto(Stall stall);
}
