package nl.saxion.concurrency.domain;

public class ReservationException extends Exception {

    /**
     * Thrown by Hotel to say why there was a problem bound to a Reservation.
     * @see Hotel
     * @see Reservation
     */
    public ReservationException(String message) {
        super(message);
    }

}
