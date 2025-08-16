package nl.saxion.concurrency.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import nl.saxion.concurrency.domain.Hotel;
import nl.saxion.concurrency.domain.Reservation;
import nl.saxion.concurrency.domain.ReservationException;
import nl.saxion.concurrency.messages.RentARoomMessage;

import java.util.ArrayList;

public class HotelManagerActor extends AbstractBehavior<RentARoomMessage> {

    /**
     * ServiceKey to notify AgentActors of HotelManagerActor constructions and deconstructions.
     */
    public static final ServiceKey<RentARoomMessage> HOTEL_MANAGER_SERVICE_KEY = ServiceKey.create(
            RentARoomMessage.class,
            "AccountManagerService");

    private final Hotel hotel;


    public HotelManagerActor(ActorContext<RentARoomMessage> context, Hotel hotel) {
        super(context);
        this.hotel = hotel;

        context.getSystem().receptionist().tell(Receptionist.register(HOTEL_MANAGER_SERVICE_KEY, context.getSelf()));
    }

    public static Behavior<RentARoomMessage> create(Hotel hotel) {
        return Behaviors.setup(context -> new HotelManagerActor(context, hotel));
    }


    @Override
    public Receive<RentARoomMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(RentARoomMessage.ListReservations.class, this::listReservations)
                .onMessage(RentARoomMessage.RequestReservations.class, this::requestReservations)
                .onMessage(RentARoomMessage.RequestHotel.class, this::requestHotel)
                .build();
    }


    private Behavior<RentARoomMessage> listReservations(RentARoomMessage.ListReservations message) {
        ArrayList<String> responses = new ArrayList<>();
        // Loop over all reservations with the customer specified in the message
        for (Reservation reservation : hotel.getReservations(message.customer).values()) {
            responses.add(reservation.toString() + ", Confirmed: " + (reservation.getConfirmed() ? "Yes" : "No"));
        }

        message.sender.tell(new RentARoomMessage.Response(
                        "You have the following reservations in " + hotel.name + ":\n" +
                        String.join("\n", responses)));
        return Behaviors.same();
    }

    private Behavior<RentARoomMessage> requestReservations(RentARoomMessage.RequestReservations message) {
        ArrayList<String> responses = new ArrayList<>();
        responses.add("Reservations for " + hotel.name + ":");
        for (Reservation reservation : message.reservations) {
            try {
                hotel.addReservation(reservation);
                responses.add("The reservation: \"" + reservation + "\" was received.");
            // Throws when the hotel doesn't have enough rooms for the reservation
            } catch (ReservationException ex) {
                responses.add(ex.getMessage());
            }
        }
        message.sender.tell(new RentARoomMessage.Response(String.join("\n", responses)));
        return Behaviors.same();
    }

    private Behavior<RentARoomMessage> requestHotel(RentARoomMessage.RequestHotel message) {
        message.sender.tell(new RentARoomMessage.SendHotel(hotel));
        return Behaviors.same();
    }

}
