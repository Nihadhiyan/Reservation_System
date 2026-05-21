package com.bookfair.backend.service;



import com.bookfair.backend.dto.request.ReservationRequest;
import com.bookfair.backend.dto.response.ReservationResponse;
import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.model.Reservation.ReservationStatus;
import com.bookfair.backend.repository.GenreRepository;
import com.bookfair.backend.repository.ReservationRepository;
import com.bookfair.backend.repository.StallRepository;
import com.bookfair.backend.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;



@Service
public class ReservationService {

    

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final StallRepository stallRepository;
    private final QRService qrCodeService;
    private final EmailService emailService;
    private final GenreRepository genreRepository;

    

    public ReservationService(ReservationRepository reservationRepository, UserRepository userRepository, StallRepository stallRepository, QRService qrCodeService, EmailService emailService, GenreRepository genreRepository) {

        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.stallRepository = stallRepository;
        this.qrCodeService = qrCodeService;
        this.emailService = emailService;
        this.genreRepository = genreRepository;
    }

    

    public List<ReservationResponse> getAllReservations() {

        return reservationRepository.findAll().stream()
                .map(reservation -> {
                    ReservationResponse reservationResponse = new ReservationResponse();
                    reservationResponse.setId(reservation.getId());
                    reservationResponse.setDate(reservation.getDate().toString());
                    reservationResponse.setTime(reservation.getTime());
                    reservationResponse.setStatus(reservation.getStatus().name());
                    reservationResponse.setUserId(reservation.getUser().getId().toString());
                    reservationResponse.setGenreId(reservation.getGenre().getId());
                    reservationResponse.setStallId(reservation.getStalls()
                        .stream()
                        .map(stall -> stall.getId())
                        .toList()
                    );
                    return reservationResponse;
                })
                .toList();

    }



    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        ReservationResponse reservationResponse = new ReservationResponse();
        
        reservationResponse.setId(reservation.getId());
        reservationResponse.setDate(reservation.getDate().toString());
        reservationResponse.setTime(reservation.getTime());
        reservationResponse.setStatus(reservation.getStatus().name());
        reservationResponse.setUserId(reservation.getUser().getId().toString());
        reservationResponse.setGenreId(reservation.getGenre().getId());
        reservationResponse.setStallId(reservation.getStalls()
            .stream()
            .map(stall -> stall.getId())
            .toList()
        );
        
        return reservationResponse;
    }



    @Transactional
    public ReservationResponse createReservation(ReservationRequest reservationRequest) {
        Reservation reservation = new Reservation();

        reservation.setUser(userRepository
            .findById(reservationRequest.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"))
        );
        reservation.setStalls(stallRepository
            .findAllById(reservationRequest.getStallId())
        );

        reservation.setDate(LocalDate.parse(reservationRequest.getDate()));
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setTime(reservationRequest.getTime());
        reservation.setGenre(genreRepository.findById(reservationRequest.getGenreId())
            .orElseThrow(() -> new RuntimeException("Genre not found"))
        );

        Reservation savedReservation = reservationRepository.save(reservation);

        String qrCode = qrCodeService.generateQRCode("RES-" + savedReservation.getId());

        try {
            emailService.sendEmail(
            savedReservation.getUser().getEmail(), 
            "Stall: " + savedReservation.getStalls().toString(), 
            qrCode); 
        } catch (Exception e) {
            System.err.println("Database saved, but email failed: " + e.getMessage());
        }
        

        ReservationResponse reservationResponse = new ReservationResponse();

        reservationResponse.setId(reservation.getId());
        reservationResponse.setDate(reservation.getDate().toString());
        reservationResponse.setTime(reservation.getTime());
        reservationResponse.setStatus(reservation.getStatus().name());
        reservationResponse.setGenreId(reservation.getGenre().getId());
        
        return reservationResponse;
    }



    public List<ReservationResponse> getReservationsByUser(Long id) {
        return reservationRepository.findByUser(userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found")))
            .stream()
            .map(reservation -> {
                ReservationResponse reservationResponse = new ReservationResponse();
                reservationResponse.setId(reservation.getId());
                reservationResponse.setDate(reservation.getDate().toString());
                reservationResponse.setTime(reservation.getTime());
                reservationResponse.setStatus(reservation.getStatus().name());
                reservationResponse.setUserId(reservation.getUser().getId().toString());
                reservationResponse.setGenreId(reservation.getGenre().getId());
                reservationResponse.setStallId(reservation.getStalls()
                    .stream()
                    .map(stall -> stall.getId())
                    .toList()
                );
                return reservationResponse;
            })
            .toList();
    }



    public void cancelReservation(Long id) {
       Reservation reservation = reservationRepository.findById(id)
               .orElseThrow(() -> new RuntimeException("Reservation not found"));
       reservation.setStatus(ReservationStatus.CANCELLED);
       reservationRepository.save(reservation);
    }

}


