package tests;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import nl.saxion.concurrency.domain.Reservation;
import nl.saxion.concurrency.messages.RentARoomMessage;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tests.TestData.*;

public class ReservationTests {

    @Test
    public void listReservations() {
        ActorSystem<RentARoomMessage> system = initTest();

        getAddHotel1(system);

        HashMap<String, ArrayList<Reservation>> reservations = new HashMap<>();
        reservations.put("h1", new ArrayList<>(Arrays.asList(
                new Reservation("c1", 5, LocalDate.of(2022, 1, 1)),
                new Reservation("c1", 10, LocalDate.of(2022, 1, 2)))));
        AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.RequestReservationsMultiHotels(sender, reservations),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join();

        String msg = getListReservations(system);
        // Use contains instead of equals because reservation Ids are random.
        assertTrue(msg.contains("Rooms: 5, Date: 01-01-2022") &&
                msg.contains("Rooms: 10, Date: 02-01-2022"));
    }

    @Test
    public void listReservationsHotelDoesntExist() {
        ActorSystem<RentARoomMessage> system = initTest();

        String msg = getListReservations(system);
        assertEquals(msg, "h1 is not in our system.");
    }

    @Test
    public void requestReservations() {
        ActorSystem<RentARoomMessage> system = initTest();

        getAddHotel1(system);

        String msg = getRequestReservations(system);
        // Use contains instead of equals because reservation Ids are random.
        assertTrue(msg.contains("Rooms: 5, Date: 01-01-2022"));
    }

    /**
     * Requests a reservation that has a higher roomCount than the hotel.
     */
    @Test
    public void requestReservationsInsufficientRooms() {
        ActorSystem<RentARoomMessage> system = initTest();

        // h1 has roomCount 10.
        getAddHotel1(system);

        HashMap<String, ArrayList<Reservation>> reservations = new HashMap<>();
        // Reservation asks for 15 rooms.
        reservations.put("h1", new ArrayList<>(Arrays.asList(new Reservation("c1", 15, LocalDate.of(2022, 1, 1)))));
        String msg =  ((RentARoomMessage.Response) AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.RequestReservationsMultiHotels(sender, reservations),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
        assertTrue(msg.contains("h1 doesn't have 15 rooms available."));
    }

    @Test
    public void confirmReservation() {
        ActorSystem<RentARoomMessage> system = initTest();

        getAddHotel1(system);
        String msg;
        // The hotel may not have been added before requesting reservations for it.
        // Keep requesting until the hotel is ready.
        do {
            msg = getRequestReservations(system);
        } while (msg.equals("h1 is not in our system."));
        // The reservation id is mentioned between char 43 and 79 of the RequestReservations response.
        String id = msg.substring(43, 79);

        msg = ((RentARoomMessage.Response) AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.ConfirmReservation(sender, id),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
        assertEquals(msg, "The reservation has been confirmed.");
    }

    @Test
    public void confirmReservationDoesntExist() {
        ActorSystem<RentARoomMessage> system = initTest();

        getAddHotel1(system);

        String msg = ((RentARoomMessage.Response) AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.ConfirmReservation(sender, "(Id that doesn't exist)"),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
        assertEquals(msg, "There is no reservation with Id: (Id that doesn't exist) in our system.");
    }

    /**
     * Checks if the reservation was actually confirmed with a ListReservations message.
     */
    @Test
    public void confirmReservationFull() {
        ActorSystem<RentARoomMessage> system = initTest();

        getAddHotel1(system);
        String msg;
        // The hotel may not have been added before requesting reservations for it.
        // Keep requesting until the hotel is ready.
        do {
            msg = getRequestReservations(system);
        } while (msg.equals("h1 is not in our system."));
        // The reservation id is mentioned between char 43 and 79 of the RequestReservations response.
        String id = msg.substring(43, 79);

        AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.ConfirmReservation(sender, id),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join();

        msg = getListReservations(system);
        assertTrue(msg.contains("Confirmed: Yes"));
    }

    @Test
    public void cancelReservation() {
        ActorSystem<RentARoomMessage> system = initTest();

        getAddHotel1(system);
        String msg;
        // The hotel may not have been added before requesting reservations for it.
        // Keep requesting until the hotel is ready.
        do {
            msg = getRequestReservations(system);
        } while (msg.equals("h1 is not in our system."));
        // The reservation id is mentioned between char 43 and 79 of the RequestReservations response.
        String id = msg.substring(43, 79);

        msg = ((RentARoomMessage.Response) AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.CancelReservation(sender, id),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
        assertEquals(msg, "The reservation has been cancelled.");
    }

    @Test
    public void cancelReservationDoesntExist() {
        ActorSystem<RentARoomMessage> system = initTest();

        getAddHotel1(system);

        String msg = ((RentARoomMessage.Response) AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.CancelReservation(sender, "(Id that doesn't exist)"),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
        assertEquals(msg, "There is no reservation with Id: (Id that doesn't exist) in our system.");
    }

    /**
     * Checks if the reservation was actually cancelled with a ListReservations message.
     */
    @Test
    public void cancelReservationFull() {
        ActorSystem<RentARoomMessage> system = initTest();

        getAddHotel1(system);
        String msg;
        do {
            msg = getRequestReservations(system);
        } while (msg.equals("h1 is not in our system."));
        // The reservation id is mentioned between char 43 and 79 of the RequestReservations response.
        String id = msg.substring(43, 79);

        AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.CancelReservation(sender, id),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join();

        msg = getListReservations(system);
        assertEquals(msg, "You have the following reservations in h1:\n");
    }

}
