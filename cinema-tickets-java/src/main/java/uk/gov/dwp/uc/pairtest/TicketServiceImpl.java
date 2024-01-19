package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;

public class TicketServiceImpl implements TicketService {
    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    private static final int ADULT_PRICE = 20;
    private static final int CHILD_PRICE = 10;
    private static final int INFANT_PRICE = 0;
    private static final int MAX_TICKETS_ALLOWED = 20;

    TicketServiceImpl(TicketPaymentService ticketPaymentService,
                      SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {
        validateAccountId(accountId);

        PurchaseInfo purchaseInfo = processTicketRequests(ticketTypeRequests);
        ticketPaymentService.makePayment(accountId, purchaseInfo.totalAmountToPay);
        seatReservationService.reserveSeat(accountId, purchaseInfo.totalSeatsToAllocate);
    }

    private PurchaseInfo processTicketRequests(TicketTypeRequest... ticketTypeRequests) {
        if (ticketTypeRequests == null || ticketTypeRequests.length < 1)
            throw new InvalidPurchaseException("No ticket request made.");

        int totalAmountToPay = 0;
        int totalSeatsToAllocate = 0;
        boolean hasAdultTicket = false;
        int totalTickets = 0;
        int infantTickets = 0;
        int adultTickets = 0;

        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            validateTicketTypeRequest(ticketTypeRequest);

            totalAmountToPay += getTicketPrice(ticketTypeRequest.getTicketType()) *
                    ticketTypeRequest.getNoOfTickets();

            if (ticketTypeRequest.getTicketType() != INFANT)
                totalSeatsToAllocate += ticketTypeRequest.getNoOfTickets();

            if (ticketTypeRequest.getTicketType() == ADULT) {
                hasAdultTicket = true;
                adultTickets += ticketTypeRequest.getNoOfTickets();
            } else if (ticketTypeRequest.getTicketType() == INFANT) {
                infantTickets += ticketTypeRequest.getNoOfTickets();
            }

            totalTickets += ticketTypeRequest.getNoOfTickets();
        }

        validateTotalTicketsCount(totalTickets);
        validatePresenceOfAdult(hasAdultTicket, totalSeatsToAllocate);
        validateInfantToAdultTicketsRatio(infantTickets, adultTickets);

        return new PurchaseInfo(totalAmountToPay, totalSeatsToAllocate);
    }

    private int getTicketPrice(TicketTypeRequest.Type type) {
        return switch (type) {
            case ADULT -> ADULT_PRICE;
            case CHILD -> CHILD_PRICE;
            case INFANT -> INFANT_PRICE;
        };
    }

    private void validateAccountId(Long accountId) throws InvalidPurchaseException {
        if (accountId < 1)
            throw new InvalidPurchaseException("Invalid Account ID.");
    }

    private void validateTotalTicketsCount(int totalTickets) throws InvalidPurchaseException {
        if (totalTickets > MAX_TICKETS_ALLOWED)
            throw new InvalidPurchaseException("Cannot purchase more than 20 tickets at " +
                    "a time.");
    }

    private void validateTicketTypeRequest(TicketTypeRequest ticketTypeRequest)
            throws InvalidPurchaseException {
        if (ticketTypeRequest == null)
            throw new InvalidPurchaseException("Ticket type request is not valid.");
        else if (ticketTypeRequest.getTicketType() == null)
            throw new InvalidPurchaseException("Ticket type is null.");
        else if (ticketTypeRequest.getNoOfTickets() < 1)
            throw new InvalidPurchaseException("Number of tickets to be purchased must " +
                    "be positive.");
    }

    private void validatePresenceOfAdult(boolean hasAdultTicket, int totalSeatsToAllocate)
            throws InvalidPurchaseException {
        if (!hasAdultTicket && totalSeatsToAllocate > 0)
            throw new InvalidPurchaseException("Cannot purchase child or infant tickets " +
                    "without purchasing an adult ticket.");
    }

    private void validateInfantToAdultTicketsRatio(int infantTickets, int adultTickets)
            throws InvalidPurchaseException {
        if (infantTickets > 0 && adultTickets == 0)
            throw new InvalidPurchaseException("Cannot purchase infant tickets without " +
                    "an associated adult ticket.");

        if (infantTickets > adultTickets)
            throw new InvalidPurchaseException("Cannot have more infants tickets than " +
                    "adult tickets.");
    }

    private static class PurchaseInfo {
        private final int totalAmountToPay;
        private final int totalSeatsToAllocate;

        PurchaseInfo(int totalAmountToPay, int totalSeatsToAllocate) {
            this.totalAmountToPay = totalAmountToPay;
            this.totalSeatsToAllocate = totalSeatsToAllocate;
        }
    }
}
