package nl.saxion.concurrency.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import nl.saxion.concurrency.domain.Hotel;
import nl.saxion.concurrency.domain.Reservation;
import nl.saxion.concurrency.domain.ReservationException;
import nl.saxion.concurrency.messages.RentARoomMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public class AgentActor extends AbstractBehavior<RentARoomMessage> {

    /**
     * ServiceKey to notify the RentARoomActor GroupRouter of AgentActor constructions and deconstructions.
     */
    public static final ServiceKey<RentARoomMessage> AGENT_ACTOR_SERVICE_KEY = ServiceKey.create(
            RentARoomMessage.class,
            "AgentActorService");

    private final HashMap<String, ActorRef<RentARoomMessage>> hotelManagerActors;


    public AgentActor(ActorContext<RentARoomMessage> context) {
        super(context);
        hotelManagerActors = new HashMap<>();

        // Make an adapter to convert messages from AGENT_ACTOR_SERVICE_KEY to UpdateHotelManagerActors messages.
        ActorRef<Receptionist.Listing> adapter = context.messageAdapter(
                Receptionist.Listing.class,
                RentARoomMessage.UpdateHotelManagerActors::new);

        context.getSystem().receptionist().tell(
                Receptionist.subscribe(HotelManagerActor.HOTEL_MANAGER_SERVICE_KEY, adapter));

        // Register this instance to AGENT_ACTOR_SERVICE_KEY for the RentARoomActor GroupRouter.
        context.getSystem().receptionist().tell(Receptionist.register(AGENT_ACTOR_SERVICE_KEY, context.getSelf()));
    }

    public static Behavior<RentARoomMessage> create() {
        return Behaviors.setup(AgentActor::new);
    }


    @Override
    public Receive<RentARoomMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(RentARoomMessage.ListHotels.class, this::listHotels)
                .onMessage(RentARoomMessage.AddHotel.class, this::addHotel)
                .onMessage(RentARoomMessage.DeleteHotel.class, this::deleteHotel)
                .onMessage(RentARoomMessage.ListAvailableRooms.class, this::listAvailableRooms)
                .onMessage(RentARoomMessage.ListReservations.class, this::listReservations)
                .onMessage(RentARoomMessage.RequestReservationsMultiHotels.class, this::requestReservationsMultiHotels)
                .onMessage(RentARoomMessage.ConfirmReservation.class, this::confirmReservation)
                .onMessage(RentARoomMessage.CancelReservation.class, this::cancelReservation)
                .onMessage(RentARoomMessage.UpdateHotelManagerActors.class, this::updateHotelManagerActors)
                .build();
    }


    private ArrayList<Hotel> getHotels() {
        // Request the hotels of all HotelManagerActors.
        ArrayList<CompletionStage<RentARoomMessage>> stages = new ArrayList<>();
        for (ActorRef<RentARoomMessage> actor : hotelManagerActors.values()) {
            stages.add(AskPattern.ask(
                    actor,
                    RentARoomMessage.RequestHotel::new,
                    Duration.ofSeconds(10),
                    getContext().getSystem().scheduler()
            ));
        }

        // Wait for responses and add the hotels in the responses to a list.
        ArrayList<Hotel> hotels = new ArrayList<>();
        for (CompletionStage<RentARoomMessage> stage : stages) {
            RentARoomMessage msg = stage.toCompletableFuture().join();
            if (msg instanceof RentARoomMessage.SendHotel) {
                hotels.add(((RentARoomMessage.SendHotel) msg).hotel);
            } else {
                getContext().getLog().error(
                        "[{}] getHotels() wrong message type: {}",
                        getContext().getSelf().path().name(),
                        msg);
            }
        }

        return hotels;
    }


    private Behavior<RentARoomMessage> listHotels(RentARoomMessage.ListHotels message) {
        ArrayList<String> responses = new ArrayList<>();
        for (Hotel hotel : getHotels()) {
            responses.add(hotel.toString());
        }

        message.sender.tell(new RentARoomMessage.Response(
                        "The following hotels are in our system:\n" +
                        String.join("\n", responses)));

        return Behaviors.same();
    }

    private Behavior<RentARoomMessage> addHotel(RentARoomMessage.AddHotel message) {
        if (hotelManagerActors.containsKey(message.name)) {
            message.sender.tell(new RentARoomMessage.Response(message.name + " is in our system already."));
            return Behaviors.same();
        }

        Hotel hotel = new Hotel(message.name, message.roomCount);
        // Spawn a HotelManagerActor to manage the hotel.
        getContext().spawn(HotelManagerActor.create(hotel), message.name);
        message.sender.tell(new RentARoomMessage.Response(message.name + " has been added."));
        return Behaviors.same();
    }

    private Behavior<RentARoomMessage> deleteHotel(RentARoomMessage.DeleteHotel message) {
        ActorRef<RentARoomMessage> actor = hotelManagerActors.remove(message.name);
        // HashMap.remove returns the removed object if successful.
        if (actor == null) {
            message.sender.tell(new RentARoomMessage.Response(message.name + " is not in our system."));
        } else {
            message.sender.tell(new RentARoomMessage.Response(message.name + " has been deleted."));
        }
        return Behaviors.same();
    }

    private Behavior<RentARoomMessage> listAvailableRooms(RentARoomMessage.ListAvailableRooms message) {
        ArrayList<String> responses = new ArrayList<>();
        for (Hotel hotel : getHotels()) {
            int availableRooms = hotel.getAvailableRooms(message.date);
            if (availableRooms >= message.minRoomCount) {
                responses.add(hotel.name + ": Available rooms: " + availableRooms);
            }
        }

        message.sender.tell(new RentARoomMessage.Response(
                        "The following hotels have enough rooms:\n" +
                        String.join("\n", responses)));
        return Behaviors.same();
    }

    private Behavior<RentARoomMessage> listReservations(RentARoomMessage.ListReservations message) {
        ActorRef<RentARoomMessage> actor = hotelManagerActors.get(message.hotelName);
        if (actor == null) {
            message.sender.tell(new RentARoomMessage.Response(message.hotelName + " is not in our system."));
        } else {
            actor.tell(message);
        }
        return Behaviors.same();
    }

    private Behavior<RentARoomMessage> requestReservationsMultiHotels(RentARoomMessage.RequestReservationsMultiHotels message) {
        ArrayList<String> responses = new ArrayList<>();

        ArrayList<CompletionStage<RentARoomMessage>> stages = new ArrayList<>();
        // message.reservations is filtered by hotel. Loop over all reservations (value) per hotel (key).
        for (Map.Entry<String, ArrayList<Reservation>> entry : message.reservations.entrySet()) {
            ActorRef<RentARoomMessage> actor = hotelManagerActors.get(entry.getKey());
            if (actor == null) {
                responses.add(entry.getKey() + " is not in our system.");
            } else {
                // Request the reservations at the hotel
                stages.add(AskPattern.ask(
                        actor,
                        sender -> new RentARoomMessage.RequestReservations(sender, entry.getValue()),
                        Duration.ofSeconds(10),
                        getContext().getSystem().scheduler()
                ));
            }
        }

        // Wait for all responses and save them in a list as info for the user.
        for (CompletionStage<RentARoomMessage> stage : stages) {
            RentARoomMessage msg = stage.toCompletableFuture().join();
            if (msg instanceof RentARoomMessage.Response) {
                responses.add(((RentARoomMessage.Response) msg).status);
            } else {
                getContext().getLog().error(
                        "[{}] requestReservationsMultiHotels() wrong message type: {}",
                        getContext().getSelf().path().name(),
                        msg);
            }
        }

        message.sender.tell(new RentARoomMessage.Response(String.join("\n", responses)));

        return Behaviors.same();
    }

    private Behavior<RentARoomMessage> confirmReservation(RentARoomMessage.ConfirmReservation message) {
        boolean confirmed = false;
        for (Hotel hotel : getHotels()) {
            if (hotel.hasReservation(message.id)) {
                // No error will throw because we checked if it exists in the if statement above.
                try {
                    hotel.confirmReservation(message.id);
                } catch (ReservationException ignored) { }

                confirmed = true;
                break;
            }
        }

        if (confirmed) {
            message.sender.tell(new RentARoomMessage.Response("The reservation has been confirmed."));
        } else {
            message.sender.tell(new RentARoomMessage.Response("There is no reservation with Id: " +
                    message.id +
                    " in our system."));
        }
        return Behaviors.same();
    }

    private Behavior<RentARoomMessage> cancelReservation(RentARoomMessage.CancelReservation message) {
        boolean cancelled = false;
        for (Hotel hotel : getHotels()) {
            if (hotel.hasReservation(message.id)) {
                // No error will throw because we checked if it exists in the if statement above.
                try {
                    hotel.deleteReservation(message.id);
                } catch (ReservationException ignored) { }

                cancelled = true;
                break;
            }
        }

        if (cancelled) {
            message.sender.tell(new RentARoomMessage.Response("The reservation has been cancelled."));
        } else {
            message.sender.tell(new RentARoomMessage.Response("There is no reservation with Id: " +
                    message.id +
                    " in our system."));
        }
        return Behaviors.same();
    }


    /**
     * Update hotelManagerActors with the Receptionist.Listing in the given message.
     */
    private Behavior<RentARoomMessage> updateHotelManagerActors(RentARoomMessage.UpdateHotelManagerActors message) {
        // Get references to all HotelManagerActors (everything registered to HOTEL_MANAGER_SERVICE_KEY).
        Set<ActorRef<RentARoomMessage>> actorSet = message.hotelManagerActors.getServiceInstances(
                HotelManagerActor.HOTEL_MANAGER_SERVICE_KEY);
        for (ActorRef<RentARoomMessage> actor : actorSet) {
            // hotelManagerActors uses the actor names as key.
            String actorName = actor.path().name();
            // Add the HotelManagerActor if it's new to the AgentActor
            if (!hotelManagerActors.containsKey(actorName)) {
                getContext().getLog().info(
                        "[{}] New HotelManager found: {}",
                        getContext().getSelf().path().name(),
                        actorName);
                hotelManagerActors.put(actorName, actor);
            }
        }

        // Remove all actors from hotelManagerActors that aren't listed in actorSet.
        hotelManagerActors.entrySet().removeIf(entry -> {
            if (!actorSet.contains(entry.getValue())) {
                getContext().getLog().info(
                        "[{}] HotelManager no longer exists: {}",
                        getContext().getSelf().path().name(),
                        entry.getKey());
                return true;
            }
            return false;
        });

        return Behaviors.same();
    }

}
