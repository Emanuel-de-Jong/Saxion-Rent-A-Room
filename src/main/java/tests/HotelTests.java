package tests;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import nl.saxion.concurrency.messages.RentARoomMessage;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static tests.TestData.*;

public class HotelTests {

    @Test
    public void listHotels() {
        ActorSystem<RentARoomMessage> system = initTest();

        getAddHotel1(system);
        getAddHotel2(system);

        String msg = ((RentARoomMessage.Response) AskPattern.ask(system,
                RentARoomMessage.ListHotels::new,
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
        assertEquals(msg, "The following hotels are in our system:\n" +
                "h1: Rooms: 10, Reservations: 0\n" +
                "h2: Rooms: 10, Reservations: 0");
    }

    @Test
    public void addHotel() {
        ActorSystem<RentARoomMessage> system = initTest();

        String msg = getAddHotel1(system);
        assertEquals(msg, "h1 has been added.");
    }

    @Test
    public void addHotelAlreadyExists() {
        ActorSystem<RentARoomMessage> system = initTest();

        // Add a hotel with h1
        getAddHotel1(system);
        String msg = ((RentARoomMessage.Response) AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                // Try to add another one
                sender -> new RentARoomMessage.AddHotel(sender, "h1", 10),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
        assertEquals(msg, "h1 is in our system already.");
    }

    @Test
    public void deleteHotel() {
        ActorSystem<RentARoomMessage> system = initTest();

        getAddHotel1(system);

        String msg = ((RentARoomMessage.Response) AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.DeleteHotel(sender, "h1"),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
        assertEquals(msg, "h1 has been deleted.");
    }

    @Test
    public void deleteHotelDoesntExist() {
        ActorSystem<RentARoomMessage> system = initTest();

        String msg = ((RentARoomMessage.Response) AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.DeleteHotel(sender, "h1"),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
        assertEquals(msg, "h1 is not in our system.");
    }

    /**
     * Checks if the hotel was actually deleted with a ListHotels message.
     */
    @Test
    public void deleteHotelFull() {
        ActorSystem<RentARoomMessage> system = initTest();

        getAddHotel1(system);

        AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.DeleteHotel(sender, "h1"),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join();

        String msg = ((RentARoomMessage.Response) AskPattern.ask(system,
                RentARoomMessage.ListHotels::new,
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
        // It's deleted if ListHotels doesn't display it anymore.
        assertEquals(msg, "The following hotels are in our system:\n");
    }

}
