// com/example/trainticketoffice/service/TicketService.java
package com.example.trainticketoffice.service;

import java.util.Map;
import java.util.Optional;

public interface TicketService {


    String createTicketForBooking(Long bookingId, Map<String, Object> requestData);

    Optional<Long> getTicketIdByCode(String code);


    boolean checkInTicket(Long ticketId);

    boolean cancelTicket(Long ticketId);
}