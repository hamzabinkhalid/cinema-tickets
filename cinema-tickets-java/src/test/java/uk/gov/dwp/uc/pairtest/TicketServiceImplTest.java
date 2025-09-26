package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;

class TicketServiceImplTest {

    private TicketServiceImpl ticketService;

    @Mock
    private SeatReservationService seatReservationService;

    @Mock
    private TicketPaymentService ticketPaymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ticketService = new TicketServiceImpl(seatReservationService, ticketPaymentService);
    }

    @Test
    void testPurchaseTicketsValid() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        assertDoesNotThrow(() -> ticketService.purchaseTickets(1L, adultRequest, childRequest, infantRequest));

        // Verify payment is called before reservation
        InOrder inOrder = inOrder(ticketPaymentService, seatReservationService);
        inOrder.verify(ticketPaymentService).makePayment(1L, 2 * 25 + 1 * 15); // 2 adults * 25 + 1 child * 15
        inOrder.verify(seatReservationService).reserveSeat(1L, 3); // 2 adults + 1 child
    }

    @Test
    void testPurchaseTicketsInvalidAccountId() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(null, request));
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(0L, request));
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(-1L, request));

        verifyNoInteractions(ticketPaymentService);
        verifyNoInteractions(seatReservationService);
    }

    @Test
    void testPurchaseTicketsNoRequests() {
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L));
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, (TicketTypeRequest[]) null));

        verifyNoInteractions(ticketPaymentService);
        verifyNoInteractions(seatReservationService);
    }

    @Test
    void testPurchaseTicketsNegativeTickets() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -1);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, request));

        verifyNoInteractions(ticketPaymentService);
        verifyNoInteractions(seatReservationService);
    }

    @Test
    void testPurchaseTicketsZeroTickets() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, request));

        verifyNoInteractions(ticketPaymentService);
        verifyNoInteractions(seatReservationService);
    }

    @Test
    void testPurchaseTicketsTooManyTickets() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, request));

        verifyNoInteractions(ticketPaymentService);
        verifyNoInteractions(seatReservationService);
    }

    @Test
    void testPurchaseTicketsNoAdults() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, request));

        verifyNoInteractions(ticketPaymentService);
        verifyNoInteractions(seatReservationService);
    }

    @Test
    void testPurchaseTicketsMoreInfantsThanAdults() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, adultRequest, infantRequest));

        verifyNoInteractions(ticketPaymentService);
        verifyNoInteractions(seatReservationService);
    }

    @Test
    void testPurchaseTicketsValidWithInfants() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        assertDoesNotThrow(() -> ticketService.purchaseTickets(1L, adultRequest, infantRequest));

        InOrder inOrder = inOrder(ticketPaymentService, seatReservationService);
        inOrder.verify(ticketPaymentService).makePayment(1L, 2 * 25); // 2 adults * 25
        inOrder.verify(seatReservationService).reserveSeat(1L, 2); // 2 adults
    }

    @Test
    void testPurchaseTicketsNullRequest() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, adultRequest, null));

        verifyNoInteractions(ticketPaymentService);
        verifyNoInteractions(seatReservationService);
    }

    @Test
    void testPurchaseTicketsNullTicketType() {
        TicketTypeRequest nullTypeRequest = new TicketTypeRequest(null, 1);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, nullTypeRequest));

        verifyNoInteractions(ticketPaymentService);
        verifyNoInteractions(seatReservationService);
    }

    @Test
    void testPurchaseTicketsBoundaryMaxTickets() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 25);

        assertDoesNotThrow(() -> ticketService.purchaseTickets(1L, adultRequest));

        InOrder inOrder = inOrder(ticketPaymentService, seatReservationService);
        inOrder.verify(ticketPaymentService).makePayment(1L, 25 * 25);
        inOrder.verify(seatReservationService).reserveSeat(1L, 25);
    }

    @Test
    void testPurchaseTicketsDuplicateTicketTypes() {
        TicketTypeRequest adultRequest1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest adultRequest2 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3);

        assertDoesNotThrow(() -> ticketService.purchaseTickets(1L, adultRequest1, adultRequest2));

        InOrder inOrder = inOrder(ticketPaymentService, seatReservationService);
        inOrder.verify(ticketPaymentService).makePayment(1L, 5 * 25); // 5 adults
        inOrder.verify(seatReservationService).reserveSeat(1L, 5);
    }

    @Test
    void testPurchaseTicketsBoundaryMaxTicketsMixedTypes() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 10);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 10);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 5);

        assertDoesNotThrow(() -> ticketService.purchaseTickets(1L, adultRequest, childRequest, infantRequest));

        InOrder inOrder = inOrder(ticketPaymentService, seatReservationService);
        inOrder.verify(ticketPaymentService).makePayment(1L, 10 * 25 + 10 * 15); // 10 adults + 10 children
        inOrder.verify(seatReservationService).reserveSeat(1L, 20); // 10 adults + 10 children
    }

}
