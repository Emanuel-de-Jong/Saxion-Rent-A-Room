package nl.saxion.concurrency.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Reservation {

    /**
     * Unique UUID to identify the reservation.
     */
    public final String id;
    /**
     * Name of the customer that made the reservation.
     */
    public final String customer;
    /**
     * Amount of rooms to reserve.
     */
    public final int roomCount;
    public final LocalDate date;

    private boolean confirmed;


    public Reservation(String customer, int roomCount, LocalDate date) {
        this.customer = customer;
        this.roomCount = roomCount;
        this.date = date;

        this.id = UUID.randomUUID().toString();
        this.confirmed = false;
    }


    public boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }


    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return String.format(
                "Id: %s, Rooms: %d, Date: %s",
                id,
                roomCount,
                date.format(formatter)
        );
    }

}
