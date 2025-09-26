package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private static final int MAX_TICKETS = 25;
    private static final int ADULT_PRICE = 25;
    private static final int CHILD_PRICE = 15;

    private final SeatReservationService seatReservationService;
    private final TicketPaymentService ticketPaymentService;

    public TicketServiceImpl(SeatReservationService seatReservationService, TicketPaymentService ticketPaymentService) {
        this.seatReservationService = seatReservationService;
        this.ticketPaymentService = ticketPaymentService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validateAccountId(accountId);
        validateRequests(ticketTypeRequests);
        int[] counts = tallyCounts(ticketTypeRequests);
        int adults = counts[0], children = counts[1], infants = counts[2];
        validateCounts(adults, children, infants);

        // Calculate seats and price
        int totalSeats = adults + children;
        int totalPrice = adults * ADULT_PRICE + children * CHILD_PRICE; // infants are free

        // Make payment first
        ticketPaymentService.makePayment(accountId, totalPrice);

        // Then reserve seats
        seatReservationService.reserveSeat(accountId, totalSeats);
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Account ID must be a positive number. Provided: " + accountId);
        }
    }

    private void validateRequests(TicketTypeRequest... ticketTypeRequests) {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("At least one ticket type request must be provided.");
        }
        for (TicketTypeRequest request : ticketTypeRequests) {
            if (request == null) {
                throw new InvalidPurchaseException("Ticket type request cannot be null.");
            }
            if (request.getTicketType() == null) {
                throw new InvalidPurchaseException("Ticket type cannot be null.");
            }
            if (request.getNoOfTickets() <= 0) {
                throw new InvalidPurchaseException("Number of tickets must be positive. Provided: " + request.getNoOfTickets());
            }
        }
    }

    private int[] tallyCounts(TicketTypeRequest... ticketTypeRequests) {
        int adults = 0, children = 0, infants = 0;
        for (TicketTypeRequest request : ticketTypeRequests) {
            switch (request.getTicketType()) {
                case ADULT -> adults += request.getNoOfTickets();
                case CHILD -> children += request.getNoOfTickets();
                case INFANT -> infants += request.getNoOfTickets();
            }
        }
        return new int[]{adults, children, infants};
    }

    private void validateCounts(int adults, int children, int infants) {
        int totalTickets = adults + children + infants;
        if (totalTickets > MAX_TICKETS) {
            throw new InvalidPurchaseException("Cannot purchase more than " + MAX_TICKETS + " tickets at once. Requested: " + totalTickets);
        }
        if (adults == 0) {
            throw new InvalidPurchaseException("At least one adult ticket must be purchased.");
        }
        if (infants > adults) {
            throw new InvalidPurchaseException("Number of infant tickets cannot exceed number of adult tickets. Infants: " + infants + ", Adults: " + adults);
        }
    }

}
