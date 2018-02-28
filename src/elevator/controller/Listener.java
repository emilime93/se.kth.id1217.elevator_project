package elevator.controller;

import elevator.Elevators;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

class Listener implements Runnable {

    ElevatorController controller;

    public Listener(ElevatorController controller) {
        this.controller = controller;
    }


    @Override
    public void run() {
        Socket socket = controller.getSocket();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String str;
            while ((str = reader.readLine()) != null) {
                String[] command = str.split(" ");
                int elevatorID;
                switch (command[0].charAt(0)) {
                    // Elevator position update
                    case 'f':
                        double position;
                        elevatorID = Integer.parseInt(command[1]);
                        position = Double.parseDouble(command[2]);
                        controller.updateElevatorPos(elevatorID, position);
                        break;
                    // When a floor button is pressed
                    case 'b':
                        int floorPressed, direction;
                        floorPressed = Integer.parseInt(command[1]);
                        direction = Integer.parseInt(command[2]);
                        controller.moveToFloor(floorPressed, direction);
                        break;
                    // When a panel button inside the elevator is pressed
                    case 'p':
                        int destination;
                        elevatorID = Integer.parseInt(command[1]);
                        destination = Integer.parseInt(command[2]);
                        controller.moveElevator(elevatorID, destination);
                        break;
                    default:
                        System.out.println("default switch clause, something is wrong");
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}