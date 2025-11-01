package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;

    @Autowired
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/bookings/{id}/tickets")
    public ResponseEntity<Map<String, String>> addTicketToBooking(
            @PathVariable("id") Long bookingId,
            @RequestBody Map<String, Object> requestBody) {


        String ticketCode = ticketService.createTicketForBooking(bookingId, requestBody);

        if (ticketCode == null) {

            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }


        return new ResponseEntity<>(Collections.singletonMap("code", ticketCode), HttpStatus.CREATED);
    }

    @GetMapping("/tickets/{code}")
    public ResponseEntity<?> getTicketByCode(@PathVariable("code") String code) {

        Optional<Long> ticketId = ticketService.getTicketIdByCode(code);

        if (ticketId.isEmpty()) {

            return ResponseEntity.notFound().build();
        }


        return ResponseEntity.ok().build();
    }

    @PutMapping("/tickets/{id}/check-in")
    public ResponseEntity<?> checkInTicket(@PathVariable("id") Long ticketId) {

        boolean success = ticketService.checkInTicket(ticketId);

        if (success) {

            return ResponseEntity.ok().build();
        } else {

            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/tickets/{id}/cancel")
    public ResponseEntity<?> cancelTicket(@PathVariable("id") Long ticketId) {
        boolean success = ticketService.cancelTicket(ticketId);

        if (success) {
            // Hủy thành công
            return ResponseEntity.ok().build();
        } else {

            return ResponseEntity.badRequest().build();
        }
    }

}