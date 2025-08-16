package nl.saxion.concurrency;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import nl.saxion.concurrency.actors.RentARoomActor;
import nl.saxion.concurrency.domain.Reservation;
import nl.saxion.concurrency.messages.RentARoomMessage;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.CompletionStage;

public class StartAkka {

    private ActorSystem<RentARoomMessage> system;


    public static void main(String[] args) {
        new StartAkka().run();
    }

    private void run() {
        system = ActorSystem.create(RentARoomActor.create(), "RentARoomSystem");
        System.out.println("System has been started.\n");

        commandLoop();

        system.terminate();
        // Tell the user when AKKA has terminated successfully
        system.getWhenTerminated().whenComplete((done, err) -> System.out.println("System has been terminated."));
    }

    /**
     * A second way to start AKKA.
     * For unit tests.
     */
    public ActorSystem<RentARoomMessage> test() {
        system = ActorSystem.create(RentARoomActor.create(), "RentARoomSystem");
        return system;
    }


    /**
     * The main loop of the program.
     * Lets the user choose a command.
     * Afterwards it gets send, the response is displayed and loop starts over
     */
    public void commandLoop() {
        String help = "Commands:\n" +
                "B: Add agent\n" +
                "L: List hotels\n" +
                "H: Add hotel\n" +
                "D: Delete hotel\n" +
                "F: List available rooms\n" +
                "E: List reservations\n" +
                "R: Request reservations\n" +
                "C: Confirm reservation\n" +
                "X: Cancel reservation\n" +
                "?: This menu\n" +
                "Q: Quit\n";
        System.out.println(help);

        String command = "";
        while (!command.equals("q")) {
            command = askString("Choose a command:", 1, 1);
            switch (command) {
                case "b":
                    addAgent();
                    break;
                case "l":
                    listHotels();
                    break;
                case "h":
                    addHotel();
                    break;
                case "d":
                    deleteHotel();
                    break;
                case "f":
                    listAvailableRooms();
                    break;
                case "e":
                    listReservations();
                    break;
                case "r":
                    requestReservations();
                    break;
                case "c":
                    confirmReservation();
                    break;
                case "x":
                    cancelReservation();
                    break;
                case "?":
                    System.out.println(help);
                    break;
            }
        }
    }


    private void addAgent() {
        CompletionStage<RentARoomMessage> stage = AskPattern.ask(
                system,
                RentARoomMessage.AddAgent::new,
                Duration.ofSeconds(10),
                system.scheduler()
        );
        awaitAndHandleStage(stage);
    }

    private void listHotels() {
        CompletionStage<RentARoomMessage> stage = AskPattern.ask(
                system,
                RentARoomMessage.ListHotels::new,
                Duration.ofSeconds(10),
                system.scheduler()
        );
        awaitAndHandleStage(stage);
    }

    private void addHotel() {
        String name = askString("Give the name of the hotel:", 2, 100);
        int roomCount = askInt("Give the number of rooms:", 1, 10_000);
        CompletionStage<RentARoomMessage> stage = AskPattern.ask(
                system,
                sender -> new RentARoomMessage.AddHotel(sender, name, roomCount),
                Duration.ofSeconds(10),
                system.scheduler()
        );
        awaitAndHandleStage(stage);
    }

    private void deleteHotel() {
        String name = askString("Give the name of the hotel:", 2, 100);
        CompletionStage<RentARoomMessage> stage = AskPattern.ask(
                system,
                sender -> new RentARoomMessage.DeleteHotel(sender, name),
                Duration.ofSeconds(10),
                system.scheduler()
        );
        awaitAndHandleStage(stage);
    }

    private void listAvailableRooms() {
        int minRoomCount = askInt("Give the minimal amount of rooms that need to be available:", 1, 1_000);
        LocalDate date = askDate("Give the date on which to find available rooms:");
        CompletionStage<RentARoomMessage> stage = AskPattern.ask(
                system,
                sender -> new RentARoomMessage.ListAvailableRooms(sender, minRoomCount, date),
                Duration.ofSeconds(10),
                system.scheduler()
        );
        awaitAndHandleStage(stage);
    }

    private void listReservations() {
        String customer = askString("Give your name:", 2, 50);
        String hotelName = askString("Give the name of the hotel:", 2, 100);
        CompletionStage<RentARoomMessage> stage = AskPattern.ask(
                system,
                sender -> new RentARoomMessage.ListReservations(sender, hotelName, customer),
                Duration.ofSeconds(10),
                system.scheduler()
        );
        awaitAndHandleStage(stage);
    }

    private void requestReservations() {
        String customer = askString("Give your name:", 2, 50);

        boolean addReservation = true;
        // all reservations filtered by hotel <hotelName, Reservations>
        HashMap<String, ArrayList<Reservation>> reservations = new HashMap<>();
        // Keep adding reservations to hotels until the user is satisfied
        while (addReservation) {
            String hotelName = askString("Give the name of the hotel:", 2, 100);

            if (!reservations.containsKey(hotelName))
                reservations.put(hotelName, new ArrayList<>());

            reservations.get(hotelName).add(
                    new Reservation(
                            customer,
                            askInt("Give the number of rooms you want to reserve:", 1, 1_000),
                            askDate("Give the date you want to reserve:")
                    )
            );

            addReservation = askBool("Do you want to add another reservation?");
        }

        CompletionStage<RentARoomMessage> stage = AskPattern.ask(
                system,
                sender -> new RentARoomMessage.RequestReservationsMultiHotels(sender, reservations),
                Duration.ofSeconds(10),
                system.scheduler()
        );

        awaitAndHandleStage(stage);
    }

    private void confirmReservation() {
        String id = askString("Give the id of the reservation:", 36, 36);
        CompletionStage<RentARoomMessage> stage = AskPattern.ask(
                system,
                sender -> new RentARoomMessage.ConfirmReservation(sender, id),
                Duration.ofSeconds(10),
                system.scheduler()
        );
        awaitAndHandleStage(stage);
    }

    private void cancelReservation() {
        String id = askString("Give the id of the reservation:", 36, 36);
        CompletionStage<RentARoomMessage> stage = AskPattern.ask(
                system,
                sender -> new RentARoomMessage.CancelReservation(sender, id),
                Duration.ofSeconds(10),
                system.scheduler()
        );
        awaitAndHandleStage(stage);
    }


    /**
     * Waits for the given stage and displays it's response.
     */
    private void awaitAndHandleStage(CompletionStage<RentARoomMessage> stage) {
        RentARoomMessage msg = stage.toCompletableFuture().join();
        // Check if the msg is of type Response. Should always be the case.
        if (msg instanceof RentARoomMessage.Response) {
            System.out.println(((RentARoomMessage.Response) msg).status);
        } else {
            System.err.println("StartAkka awaitAndHandleStage() wrong message type: " + msg);
        }
        System.out.println();
    }


    /**
     * Asks the given question and lets the user input a string until it's valid.
     * @param question The question to ask.
     * @param minLength The min length of the input string.
     * @param maxLength The max length of the input string.
     */
    private static String askString(String question, int minLength, int maxLength) {
        System.out.println(question);

        String input = "";
        boolean inputValid = false;
        Scanner scanner = new Scanner(System.in);
        while (!inputValid) {
            input = scanner.nextLine();

            if (input.length() < minLength || input.length() > maxLength) {
                System.err.printf("Your answer needs to be between %d and %d characters.", minLength, maxLength);
                continue;
            }

            inputValid = true;
        }

        return input;
    }

    /**
     * Asks the given question and lets the user input an int until it's valid.
     * @param question The question to ask.
     * @param minLength The min length of the input int.
     * @param maxLength The max length of the input int.
     */
    private static int askInt(String question, int minLength, int maxLength) {
        System.out.println(question);

        int input = 0;
        boolean inputValid = false;
        Scanner scanner = new Scanner(System.in);
        while (!inputValid) {
            try {
                input = scanner.nextInt();
            } catch (Exception ex) {
                System.err.println("Your answer needs to be a number.");
                continue;
            }

            if (input < minLength || input > maxLength) {
                System.err.printf("The number needs to be in the range %d - %d.", minLength, maxLength);
                continue;
            }

            inputValid = true;
        }

        return input;
    }

    /**
     * Asks the given question and lets the user input a boolean until it's valid.
     * @param question The question to ask.
     */
    private static boolean askBool(String question) {
        System.out.println(question + " (y/n)");

        String input;
        boolean result = false;
        boolean inputValid = false;
        Scanner scanner = new Scanner(System.in);
        while (!inputValid) {
            input = scanner.nextLine();
            input = input.toLowerCase();

            if (input.contains("y")) {
                result = true;
                inputValid = true;
            } else if (input.contains("n")) {
                inputValid = true;
            } else {
                System.err.println("Your answer needs to be 'y' or 'n'.");
            }
        }

        return result;
    }

    /**
     * Asks the given question and lets the user input a date until it's valid.
     * Only accepts dates in the future.
     * @param question The question to ask.
     */
    private static LocalDate askDate(String question) {
        System.out.println(question + " (dd-mm-yyyy)");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        String input;
        LocalDate date = null;
        boolean inputValid = false;
        Scanner scanner = new Scanner(System.in);
        while (!inputValid) {
            input = scanner.nextLine();
            try {
                date = LocalDate.parse(input, formatter);
            } catch (Exception ex) {
                System.err.println("Your answer needs to be in the right date format.");
                continue;
            }

            // Check if the date is in the past.
            if (date.compareTo(LocalDate.now()) < 0) {
                System.err.println("The date needs to be in the future.");
                continue;
            }

            inputValid = true;
        }

        return date;
    }

}
