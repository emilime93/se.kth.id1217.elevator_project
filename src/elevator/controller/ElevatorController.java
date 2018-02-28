package elevator.controller;

import elevator.Elevators;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ElevatorController implements Runnable {

    private Socket socket;
    private PrintWriter writer;
    private ElevatorState[] elevatorStates;

    public ElevatorController() {
    }

    void updateElevatorPos(int elevatorID, double pos) {
        elevatorStates[elevatorID-1].setFloor(pos);
    }

    void printStates() {
        for (ElevatorState e : elevatorStates)
            System.out.println(e);
        System.out.println();
    }

    private void initiateStates() {
        for (int i = 0; i < elevatorStates.length; i++) {
            elevatorStates[i] = new ElevatorState(0, 0, i+1, this);
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
            } catch (InterruptedException e) {
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

    void moveElevator(int elevatorID, int destination) {
        // TODO ...
        elevatorStates[elevatorID-1].addRequest(destination,);
        for (ElevatorState e : elevatorStates) {
            e.printMyQueues();
        }
    }


    // The great algorithm
    void moveToFloor(int destination, int direction) {
        double shortestDistance = Double.MAX_VALUE;
        int shortestID = 0;

        for (ElevatorState e : elevatorStates) {
            double dist = e.calculateDistance(destination, direction);
            if (dist < shortestDistance) {
                shortestDistance = dist;
                shortestID = e.getId();
            }
        }
        elevatorStates[shortestID-1].addRequest(destination, direction);

        // For debug
        for (ElevatorState e : elevatorStates) {
            e.printMyQueues();
        }
    }
}