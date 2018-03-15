package elevator.controller;

import elevator.Elevators;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class ElevatorController implements Runnable {

    private Socket socket;
    private PrintWriter writer;
    private ElevatorState[] elevatorStates;

    public ElevatorController() {
    }

    void updateElevatorPos(int elevatorID, int pos) {
        elevatorStates[elevatorID-1].setFloor(pos);
    }

    void printStates() {
        for (ElevatorState e : elevatorStates)
            System.out.println(e);
        System.out.println();
    }

    private void initiateStates() {
        for (int i = 0; i < elevatorStates.length; i++) {
            elevatorStates[i] = new ElevatorState( 0, i+1, this);
            new Thread(elevatorStates[i]).start();
        }

    }

    public void sendCommand(String command) {
        writer.println(command);
        writer.flush();
    }

    public Socket getSocket() {
        return this.socket;
    }

    @Override
    public void run() {
        initiateController();
        initiateStates();
    }

    private void initiateController() {
        elevatorStates = new ElevatorState[Elevators.numberOfElevators];
        System.out.println("Number of elevators: " + elevatorStates.length);
        do {
            try {
                Thread.sleep(100);
                InetAddress inetAddress = InetAddress.getByName("localhost");
                socket = new Socket(inetAddress, Elevators.defaultPort);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
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
        int bestRank = 10000;
        int bestID = 0;

        for (ElevatorState e : elevatorStates) {
            int rank = e.calcStopsBeforeService(destination, direction);
            if (e.isIdle()) {
                bestID = e.getId();
                break;
            }
            if (rank < bestRank) {
                bestRank = rank;
                bestID = e.getId();
            }
        }

        elevatorStates[bestID-1].addRequest(destination, direction);

        // For debug
        for (ElevatorState e : elevatorStates) {
            System.out.println(e);
            e.printMyQueues();
            System.out.println();
        }
    }

    public void stop(int elevatorID) {
        elevatorStates[elevatorID-1].stop();
    }
}