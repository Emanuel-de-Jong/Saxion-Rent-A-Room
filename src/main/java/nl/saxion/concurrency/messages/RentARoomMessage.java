package nl.saxion.concurrency.messages;

import akka.actor.typed.ActorRef;
import akka.actor.typed.receptionist.Receptionist;
import nl.saxion.concurrency.domain.Hotel;
import nl.saxion.concurrency.domain.Reservation;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

public interface RentARoomMessage extends Serializable {

    /**
     * The response of a message send from StartAkka.
     * It includes one of the following:
     *      a user friendly status message.
     *      the requested data in a user friendly text form.
     *      a user friendly error message if something went wrong.
     */
    class Response implements RentARoomMessage {
        public final String status;

        public Response(String status) {
            this.status = status;
        }
    }


    /**
     * Message to add an agent to the RentARoomAgent GroupRouter.
     */
    class AddAgent implements RentARoomMessage {
        public final ActorRef<RentARoomMessage> sender;

        public AddAgent(ActorRef<RentARoomMessage> sender) {
            this.sender = sender;
        }
    }

    /**
     * Message to list all hotels in the system.
     */
    class ListHotels implements RentARoomMessage {
        public final ActorRef<RentARoomMessage> sender;

        public ListHotels(ActorRef<RentARoomMessage> sender) {
            this.sender = sender;
        }
    }

    /**
     * Message to add a HotelManagerAgent with a hotel.
     */
    class AddHotel implements RentARoomMessage {
        public final ActorRef<RentARoomMessage> sender;
        public final String name;
        public final int roomCount;

        public AddHotel(ActorRef<RentARoomMessage> sender, String name, int roomCount) {
            this.sender = sender;
            this.name = name;
            this.roomCount = roomCount;
        }
    }

    /**
     * Message to remove a HotelManagerAgent with its hotel.
     */
    class DeleteHotel implements RentARoomMessage {
        public final ActorRef<RentARoomMessage> sender;
        public final String name;

        public DeleteHotel(ActorRef<RentARoomMessage> sender, String hotelName) {
            this.sender = sender;
            this.name = hotelName;
        }
    }


    /**
     * Message to list all hotels that have at least the given minRoomCount available at the given date.
     */
    class ListAvailableRooms implements RentARoomMessage {
        public final ActorRef<RentARoomMessage> sender;
        public final int minRoomCount;
        public final LocalDate date;

        public ListAvailableRooms(ActorRef<RentARoomMessage> sender, int minRoomCount, LocalDate date) {
            this.sender = sender;
            this.minRoomCount = minRoomCount;
            this.date = date;
        }
    }

    /**
     * Message for an AgentActor to request a hotel from a HotelManagerActor.
     */
    class RequestHotel implements RentARoomMessage {
        public final ActorRef<RentARoomMessage> sender;

        public RequestHotel(ActorRef<RentARoomMessage> sender) {
            this.sender = sender;
        }
    }

    /**
     * Message to send the HotelManagerActor's hotel to the AgentActor that requested it.
     * @see RequestHotel
     */
    class SendHotel implements RentARoomMessage {
        public final Hotel hotel;

        public SendHotel(Hotel hotel) {
            this.hotel = hotel;
        }
    }


    /**
     * Message to list all reservations in the given hotel.
     */
    class ListReservations implements RentARoomMessage {
        public final ActorRef<RentARoomMessage> sender;
        public final String hotelName;
        public final String customer;

        public ListReservations(ActorRef<RentARoomMessage> sender, String hotelName, String customer) {
            this.sender = sender;
            this.hotelName = hotelName;
            this.customer = customer;
        }
    }

    /**
     * Message to request multiple reservations for multiple hotels.
     */
    class RequestReservationsMultiHotels implements RentARoomMessage {
        public final ActorRef<RentARoomMessage> sender;
        public final HashMap<String, ArrayList<Reservation>> reservations;

        public RequestReservationsMultiHotels(ActorRef<RentARoomMessage> sender, HashMap<String, ArrayList<Reservation>> reservations) {
            this.sender = sender;
            this.reservations = reservations;
        }
    }

    /**
     * Message to request reservations for a single hotel.
     * The RequestReservationsMultiHotels message gets split up into messages of this type in AgentActor.
     * @see RequestReservationsMultiHotels
     */
    class RequestReservations implements RentARoomMessage {
        public final ActorRef<RentARoomMessage> sender;
        public final ArrayList<Reservation> reservations;

        public RequestReservations(ActorRef<RentARoomMessage> sender, ArrayList<Reservation> reservations) {
            this.sender = sender;
            this.reservations = reservations;
        }
    }

    /**
     * Message to confirm a reservation.
     */
    class ConfirmReservation implements RentARoomMessage {
        public final ActorRef<RentARoomMessage> sender;
        public final String id;

        public ConfirmReservation(ActorRef<RentARoomMessage> sender, String id) {
            this.sender = sender;
            this.id = id;
        }
    }

    /**
     * Message to cancel a reservation.
     */
    class CancelReservation implements RentARoomMessage {
        public final ActorRef<RentARoomMessage> sender;
        public final String id;

        public CancelReservation(ActorRef<RentARoomMessage> sender, String id) {
            this.sender = sender;
            this.id = id;
        }
    }


    /**
     * Message with the current receptionist list of HotelManagerActors.
     * Gets send to all AgentActors to update their private HotelManagerActor list.
     */
    class UpdateHotelManagerActors implements RentARoomMessage {
        public final Receptionist.Listing hotelManagerActors;

        public UpdateHotelManagerActors(Receptionist.Listing hotelManagerActors) {
            this.hotelManagerActors = hotelManagerActors;
        }
    }

}
