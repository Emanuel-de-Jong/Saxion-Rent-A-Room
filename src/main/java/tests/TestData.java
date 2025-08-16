package tests;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import nl.saxion.concurrency.StartAkka;
import nl.saxion.concurrency.domain.Reservation;
import nl.saxion.concurrency.messages.RentARoomMessage;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class TestData {

    /**
     * Starts the program.
     * It sleeps for 1 second to give actors time to initialize and register.
     * @return the main ActorSystem of the program.
     */
    public static ActorSystem<RentARoomMessage> initTest() {
        ActorSystem<RentARoomMessage> system = new StartAkka().test();
        try { Thread.sleep(1000); } catch (Exception ignored) { }
        return system;
    }

    /**
     * Adds a hotel with name "h1" and roomCount 10.
     * @return The response message.
     */
    public static String getAddHotel1(ActorSystem<RentARoomMessage> system) {
        return ((RentARoomMessage.Response) AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.AddHotel(sender, "h1", 10),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
    }

    /**
     * Adds a hotel with name "h2" and roomCount 10.
     * @return The response message.
     */
    public static String getAddHotel2(ActorSystem<RentARoomMessage> system) {
        return ((RentARoomMessage.Response) AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.AddHotel(sender, "h2", 10),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
    }

    /**
     * Adds a reservation with customer "c1" roomCount 5 and date 01-01-2022.
     * @return The response message.
     */
    public static String getRequestReservations(ActorSystem<RentARoomMessage> system) {
        HashMap<String, ArrayList<Reservation>> reservations = new HashMap<>();
        reservations.put("h1", new ArrayList<>(Arrays.asList(new Reservation("c1", 5, LocalDate.of(2022, 1, 1)))));
        return ((RentARoomMessage.Response) AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.RequestReservationsMultiHotels(sender, reservations),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
    }

    /**
     * Asks for the reservations for hotelName "h1" and customer "c1".
     * @return The response message.
     */
    public static String getListReservations(ActorSystem<RentARoomMessage> system) {
        return ((RentARoomMessage.Response) AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.ListReservations(sender, "h1", "c1"),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
    }

}
