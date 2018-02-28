package elevator.controller;

import elevator.Elevator;
import elevator.Elevators;

import java.util.LinkedList;
import java.util.Queue;

public class ElevatorState implements Runnable {

    private int direction;
    private double floor;
    private int id;

    private LinkedList<Integer> upQueue;
    private LinkedList<Integer> downQueue;

    private ElevatorController controller;

    ElevatorState(int direction, double floor, int id, ElevatorController controller) {
        this.direction = direction;
        this.floor = floor;
        this.id = id;
        this.controller = controller;

        upQueue = new LinkedList<>();
        downQueue = new LinkedList<>();
    }

    public void addDownRequest(int level) {
        if (downQueue.contains(level))
            return;
        for (int i = 0; i < downQueue.size(); i++) {
            if (level > downQueue.get(i)) {
                downQueue.add(i, level);
                return;
            }
        }
        downQueue.add(level);
    }

    public void addUpRequest(int level) {
        if (upQueue.contains(level))
            return;
        for (int i = 0; i < upQueue.size(); i++) {
            if (level < upQueue.get(i)) {
                upQueue.add(i, level);
                return;
            }
        }
        upQueue.add(level);
    }

    public double calculateDistance(int targetFloor, int direction) {
        // TODO take direction into consideration
        double sum = 0.0;

        if (this.direction == Elevators.UP) {  // If we're going up
            // Optimization if we are already on our way there, in the right direction
            if (upQueue.contains(targetFloor) && direction == this.direction)
                return 0.0;
            for (int i = 0; i < upQueue.size(); i++) {
                // If we found it!
                if (upQueue.get(i) > targetFloor && direction == this.direction) {

                }
                if (i == 0) {
                    sum += upQueue.get(i) - floor;
                } else {
                    sum += upQueue.get(i) - upQueue.get(i-1);
                }
            }

            if (downQueue.contains(targetFloor) && direction == this.direction*-1)
                return 0.0;

        } else {    // If we're going up
            // Optimization if we are already on our way there, in the right direction
            if (downQueue.contains(targetFloor) && direction == this.direction)
                return 0.0;
            for (int i = 0; i < downQueue.size(); i++) {
                if (i == 0) {
                    sum += floor - downQueue.get(i);
                } else {
                    sum += downQueue.get(i-1) - downQueue.get(i);
                }
            }
        }

        return sum;
    }


    void addRequest(int destination, int direction) {
        if (direction == Elevators.UP)
            addUpRequest(destination);
        else
            addDownRequest(destination);
    }

    void stop() {
        controller.sendCommand("m " + id + " 0");
    }

    void toggleDoors() {
        controller.sendCommand("d " + id + " 1");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        controller.sendCommand("d " + id + " -1");
        try {   // For good looks
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    int getId() {
        return id;
    }

    int getDirection() {
        return direction;
    }

    void setDirection(int direction) {
        this.direction = direction;
    }

    double getFloor() {
        return floor;
    }

    void setFloor(double floor) {
        this.floor = floor;
    }

    @Override
    public String toString() {
        return String.format("Elevator #%d, at floor %f, moving %d", id, floor, direction);
    }

    public void printMyQueues() {
        System.out.println("ELEVATOR #"+id);
        System.out.printf("Upqueue:\n[");
        for (Integer i : upQueue) {
            System.out.printf("%d, ", i);
        }
        System.out.printf("]\nDownqueue:\n[");
        for (Integer i : downQueue) {
            System.out.printf("%d, ", i);
        }
        System.out.printf("]\n");
    }

    @Override
    public void run() {
        int target = -1;
        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (direction == Elevators.UP) {
                // Check to see if it reached it's floor
                if (reachedFloor(target)) {
                    stop();
                    toggleDoors();
                    upQueue.pollFirst();
                    target = -1;
                }

                // Check to see if it's currently moving towards its target. If not, get the next command
                if (upQueue.peek() != null) {
                    if (target == upQueue.peek())
                        continue;
                    target = upQueue.peek();
                    direction = Elevators.UP;
                    controller.sendCommand("m " + id + " " + direction);
                } else if (downQueue.peek() != null) {
                    target = downQueue.peek();
                    direction = Elevators.DOWN;
                    controller.sendCommand("m " + id + " " + direction);
                }

            } else if (direction == Elevators.DOWN) { // If it's going down
                // Check to see if it reached it's floor
                if (reachedFloor(target)) {
                    stop();
                    toggleDoors();
                    downQueue.pollFirst();
                    target = -1;
                }

                // Check to see if it's currently moving towards its target. If not, get the next command
                if (downQueue.peek() != null) {
                    if (target == downQueue.peek())
                        continue;
                    target = downQueue.peek();
                    direction = Elevators.DOWN;
                    controller.sendCommand("m " + id + " " + direction);
                }
                else if (upQueue.peek() != null) {
                    target = upQueue.peek();
                    direction = Elevators.UP;
                    controller.sendCommand("m " + id + " " + direction);
                }
            } else { // TODO THIS IS WRONG I THINK
                if (upQueue.peek() != null) {
                    target = upQueue.peek();
                    direction = Elevators.UP;
                } else if (downQueue.peek() != null) {
                    target = downQueue.peek();
                    direction = Elevators.DOWN;
                }
            }

        }
    }

    private boolean reachedFloor(int target) {
        // Test value, may be subject to change TODO!!!
        if (Math.abs(floor - target) < 0.1) {
            return true;
        }
        return false;
    }
}