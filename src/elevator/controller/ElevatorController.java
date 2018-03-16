package elevator.controller;

import elevator.Elevators;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Title:        Elevator Main Controller
 * Description:  The main controller which is the bridge between the listener
 *               and the elevator states. It also sets up the networks connections
 *               on which the listener operates on.
 * Company:      KTH
 * Course:       ID1217 Concurrent Programming.
 *
 * @authors Emil Lindholm Brandt & Sabina Hauzenberger
 * @version 1.0
 */

public class ElevatorController implements Runnable {

    private Socket socket;
    private PrintWriter writer;
    private ElevatorState[] elevatorStates;

    public ElevatorController() {
    }

    /**
     * Updates the elevator position.
     * @param elevatorID The elevator in mind.
     * @param pos The new position/floor.
     */
    void updateElevatorPos(int elevatorID, int pos) {
        elevatorStates[elevatorID-1].setFloor(pos);
    }

    /**
     * Initiates the states of every elevator. They are given an ID ranging
     * from 1-N, and every elevator starts at floor 0 and direction UP.
     * Starts every elevator state as a thread as well.
     */
    private void initiateStates() {
        for (int i = 0; i < elevatorStates.length; i++) {
            elevatorStates[i] = new ElevatorState( 0, i+1, this);
            new Thread(elevatorStates[i]).start();
        }

    }

    /**
     * Sends a String command through the TCP connection.
     * @param command The command to send to the main application.
     */
    public void sendCommand(String command) {
        writer.println(command);
        writer.flush();
    }

    /**
     * Gives the socket on which to operate to communicate with the main program.
     * @return The socket to use.
     */
    public Socket getSocket() {
        return this.socket;
    }

    /**
     * The initial setup for the elevator once started.
     */
    @Override
    public void run() {
        initiateController();
        initiateStates();
    }

    /**
     * Initiates the controller.
     * Creates the elevator states and sets up the TCP socket and starts the Listener.
     */
    private void initiateController() {
        elevatorStates = new ElevatorState[Elevators.numberOfElevators];
        System.out.println("Number of elevators: " + elevatorStates.length);
        do {
            try {
                Thread.sleep(100);
                InetAddress inetAddress = InetAddress.getByName("localhost");
                socket = new Socket(inetAddress, Elevators.defaultPort);
            } catch (IOException e) {
//                e.printStackTrace();
            } catch (InterruptedException e) {
//                e.printStackTrace();
            }
        } while (socket == null);
        System.out.println("socket was connected");

        this.writer = null;
        try {
            writer = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(new Listener(this)).start();
    }

    /**
     * When a button is pressed inside a elevator.
     * @param elevatorID Which elevator pressed the button
     * @param destination Which floor the elevator wishes to go to
     */
    void moveElevator(int elevatorID, int destination) {
        elevatorStates[elevatorID-1].addRequest(destination);
        for (ElevatorState e : elevatorStates) {
            System.out.println(e);
            e.printMyQueues();
            System.out.println();
        }
    }


    /**
     * The great algorithm for deciding which elevator will server which request.
     * @param destination The destination floor
     * @param direction Which direction caller in mind wants to travel in
     */
    void moveToFloor(int destination, int direction) {
        int lowestCost = 10000;
        int bestID = 0;

        for (ElevatorState e : elevatorStates) {
            int cost = e.calcStopsBeforeService(destination, direction);
            System.out.printf("Elevator %d has cost %d\n", e.getId(), cost);
            if (cost < lowestCost) {
                lowestCost = cost;
                bestID = e.getId();
            }
        }

        System.out.printf("===> ELEVATOR #%d got the job!\n", bestID);

        elevatorStates[bestID-1].addRequest(destination, direction);

        // For debug
        for (ElevatorState e : elevatorStates) {
            System.out.println(e);
            e.printMyQueues();
            System.out.println();
        }
    }

    /**
     * Sends a stop signal to a elevator, stopping it.
     * @param elevatorID The ID of the elevator which to stop.
     */
    public void stop(int elevatorID) {
        elevatorStates[elevatorID-1].stop();
    }
}