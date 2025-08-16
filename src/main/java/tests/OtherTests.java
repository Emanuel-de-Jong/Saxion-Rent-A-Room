package tests;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import nl.saxion.concurrency.messages.RentARoomMessage;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tests.TestData.*;

public class OtherTests {

    @Test
    public void addAgent() {
        ActorSystem<RentARoomMessage> system = initTest();

        String msg = ((RentARoomMessage.Response) AskPattern.ask(system,
                RentARoomMessage.AddAgent::new,
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
        assertEquals(msg, "A new agent has been added.");
    }

    @Test
    public void listAvailableRooms() {
        ActorSystem<RentARoomMessage> system = initTest();

        // These hotels have roomCount 10.
        getAddHotel1(system);
        getAddHotel2(system);

        String msg = ((RentARoomMessage.Response) AskPattern.<RentARoomMessage, RentARoomMessage>ask(system,
                sender -> new RentARoomMessage.ListAvailableRooms(sender, 5, LocalDate.of(2022, 1, 1)),
                Duration.ofSeconds(10), system.scheduler()).toCompletableFuture().join()).status;
        assertEquals(msg, "The following hotels have enough rooms:\n" +
                "h1: Available rooms: 10\n" +
                "h2: Available rooms: 10");
    }

}
