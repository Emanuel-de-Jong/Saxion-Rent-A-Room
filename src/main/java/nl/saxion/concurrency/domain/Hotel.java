package nl.saxion.concurrency.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;

public class Hotel implements Serializable {

    public final String name;
    /**
     * Amount of rooms the hotel can reserve
     */
    public final int roomCount;
    private final HashMap<String, Reservation> reservations;


    public Hotel(String name, int roomCount) {
        this.name = name;
        this.roomCount = roomCount;

        this.reservations = new HashMap<>();
    }


    public HashMap<String, Reservation> getReservations(String customer) {
        // Clone the class reservation list so it doesn't get modified on the next line.
        HashMap<String, Reservation> filtered = (HashMap<String, Reservation>) reservations.clone();
        // Remove all reservations that don't have the given customer.
        filtered.entrySet().removeIf(entry -> !entry.getValue().customer.equals(customer));
        return filtered;
    }

    public boolean hasReservation(String reservationId) {
        return reservations.containsKey(reservationId);
    }

    public void addReservation(Reservation reservation) throws ReservationException {
        // Check if the reservation asks for more rooms than is available on its date.
        if (getAvailableRooms(reservation.date) - reservation.roomCount < 0) {
            throw new ReservationException(name + " doesn't have " + reservation.roomCount + " rooms available.");
        }
        reservations.put(reservation.id, reservation);
    }

    public void deleteReservation(String reservationId) throws ReservationException {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) {
            throw new ReservationException(String.format(
                    "There is no reservation with Id: %s in %s.",
                    reservationId, name));
        }
        reservations.remove(reservationId);
    }


    public void confirmReservation(String reservationId) throws ReservationException {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null)
            throw new ReservationException(String.format(
                    "There is no reservation with Id: %s in %s.",
                    reservationId, name));

        reservation.setConfirmed(true);
    }


    /**
     * @return Amount of rooms that don't have a reservation on the given date.
     */
    public int getAvailableRooms(LocalDate date) {
        int availableRooms = this.roomCount;
        for (Reservation reservation : reservations.values()) {
            if (reservation.date == date) {
                availableRooms -= reservation.roomCount;
            }
        }
        return availableRooms;
    }


    @Override
    public String toString() {
        return String.format(
                "%s: Rooms: %d, Reservations: %d",
                name,
                roomCount,
                reservations.size()
        );
    }

}
