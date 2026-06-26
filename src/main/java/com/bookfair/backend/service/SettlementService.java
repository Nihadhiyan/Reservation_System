package com.bookfair.backend.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.exception.BusinessException;
import com.bookfair.backend.exception.ErrorCode;
import com.bookfair.backend.model.Event;
import com.bookfair.backend.model.EventSettlement;
import com.bookfair.backend.model.Reservation;
import com.bookfair.backend.model.TransactionHistory;
import com.bookfair.backend.repository.EventSettlementRepository;
import com.bookfair.backend.repository.TransactionHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final EventSettlementRepository settlementRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final PricingEngineService pricingEngineService;

    @Transactional
    public EventSettlement initializeSettlement(Event event) {
        if (settlementRepository.findByEventId(event.getId()).isPresent()) {
            throw new BusinessException("Settlement already initialized for this event", ErrorCode.BUSINESS_RULE_VIOLATION);
        }

        EventSettlement settlement = new EventSettlement();
        settlement.setEvent(event);
        settlement.setOrganizer(event.getOrganizer());
        settlement.setVenueOwner(event.getVenue().getOwner());
        
        // Snapshot fields
        settlement.setSnapshottedDailyRentRate(event.getVenue().getDailyRentRate() != null ? event.getVenue().getDailyRentRate() : BigDecimal.ZERO);
        settlement.setSnapshottedRentType(event.getVenue().getRentType() != null ? event.getVenue().getRentType() : com.bookfair.backend.model.Venue.RentType.FLAT_DAILY);

        // Calculate Rent
        int durationDays = (int) java.time.Duration.between(event.getStartDateTime(), event.getEndDateTime()).toDays();
        if (durationDays <= 0) durationDays = 1;
        
        BigDecimal totalRent = pricingEngineService.calculateVenueRent(event.getVenue(), durationDays);
        settlement.setTotalRentOwed(totalRent);
        settlement.setRemainingBalance(totalRent);
        settlement.setStatus(totalRent.compareTo(BigDecimal.ZERO) == 0 ? EventSettlement.SettlementStatus.LIABILITY_COVERED : EventSettlement.SettlementStatus.LIABILITY_PENDING);

        return settlementRepository.save(settlement);
    }

    @Transactional
    public void processVendorPayment(Reservation reservation, BigDecimal amount) {
        Event event = reservation.getEvent();
        
        // 1. Log incoming transaction from Vendor to Organizer
        TransactionHistory vendorTx = new TransactionHistory();
        vendorTx.setEvent(event);
        vendorTx.setAmount(amount);
        vendorTx.setSourceRole(TransactionHistory.TransactionRole.VENDOR);
        vendorTx.setDestinationRole(TransactionHistory.TransactionRole.ORGANIZER);
        vendorTx.setReservation(reservation);
        vendorTx.setDescription("Vendor payment for reservation " + reservation.getId());
        transactionHistoryRepository.save(vendorTx);

        // 2. Fetch or initialize Settlement
        EventSettlement settlement = settlementRepository.findByEventId(event.getId())
                .orElseGet(() -> initializeSettlement(event));

        // 3. Waterfall Logic
        BigDecimal remainingRent = settlement.getRemainingBalance();
        
        if (remainingRent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal amountToCover = remainingRent.min(amount);
            
            settlement.setRemainingBalance(remainingRent.subtract(amountToCover));
            settlement.setAmountPaidToOwner(settlement.getAmountPaidToOwner().add(amountToCover));
            
            // Log Rent Coverage transaction (Organizer -> Venue Owner)
            TransactionHistory coverageTx = new TransactionHistory();
            coverageTx.setEvent(event);
            coverageTx.setAmount(amountToCover);
            coverageTx.setSourceRole(TransactionHistory.TransactionRole.ORGANIZER);
            coverageTx.setDestinationRole(TransactionHistory.TransactionRole.VENUE_OWNER);
            coverageTx.setDescription("Automatic Rent Coverage Waterfall for Event " + event.getId());
            transactionHistoryRepository.save(coverageTx);

            BigDecimal leftover = amount.subtract(amountToCover);
            if (leftover.compareTo(BigDecimal.ZERO) > 0) {
                settlement.setOrganizerProfit(settlement.getOrganizerProfit().add(leftover));
            }

            // Check if liability is crossed
            if (settlement.getRemainingBalance().compareTo(BigDecimal.ZERO) == 0 && settlement.getStatus() == EventSettlement.SettlementStatus.LIABILITY_PENDING) {
                settlement.setStatus(EventSettlement.SettlementStatus.LIABILITY_COVERED);
                log.info("Audit: Liability Covered threshold crossed for Event {}", event.getId());
                
                // Log this explicitly in TransactionHistory as an audit event
                TransactionHistory auditTx = new TransactionHistory();
                auditTx.setEvent(event);
                auditTx.setAmount(BigDecimal.ZERO);
                auditTx.setSourceRole(TransactionHistory.TransactionRole.PLATFORM);
                auditTx.setDestinationRole(TransactionHistory.TransactionRole.ORGANIZER);
                auditTx.setDescription("Liability Covered threshold crossed");
                transactionHistoryRepository.save(auditTx);
            }
        } else {
            // Rent is fully covered, 100% goes to profit
            settlement.setOrganizerProfit(settlement.getOrganizerProfit().add(amount));
            
            if (settlement.getStatus() == EventSettlement.SettlementStatus.LIABILITY_COVERED) {
                settlement.setStatus(EventSettlement.SettlementStatus.PROFIT_RELEASED);
            }
        }
        
        settlementRepository.save(settlement);
    }

    @Transactional(readOnly = true)
    public boolean canOrganizerWithdraw(Event event, BigDecimal withdrawalAmount) {
        EventSettlement settlement = settlementRepository.findByEventId(event.getId())
                .orElseThrow(() -> new BusinessException("Settlement not found", ErrorCode.BUSINESS_RULE_VIOLATION));
                
        if (settlement.getStatus() != EventSettlement.SettlementStatus.PROFIT_RELEASED && settlement.getStatus() != EventSettlement.SettlementStatus.LIABILITY_COVERED) {
            throw new BusinessException("Cannot withdraw funds: Liability not yet covered for Event " + event.getId(), ErrorCode.BUSINESS_RULE_VIOLATION);
        }
        
        return settlement.getOrganizerProfit().compareTo(withdrawalAmount) >= 0;
    }
}
