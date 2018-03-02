package elevator.controller;

import elevator.Elevator;
import elevator.Elevators;

import java.util.LinkedList;
import java.util.Queue;

public class ElevatorState implements Runnable {

    /* The floor variable will repserent level 4 as 400 etc, due to double vs integer rounding comparison problems */
    private int floor;
    /* 1 = UP | -1 = DOWN */
    private int direction;
    /* Indicates which queue we are working with at the moment */
    private int workingQueue = 1;
    /* My ID, ranges from 1 to N */
    private int id;

    private LinkedList<Integer> upQueue;
    private LinkedList<Integer> downQueue;

    private ElevatorController controller;
    private boolean idle = true;

    ElevatorState(int direction, int floor, int id, ElevatorController controller) {
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

    /**
     * Used for when someone outside the elevator wants to travel in a direction
     * @param destination The floor where the button was pressed
     * @param direction The direction they want to travel in
     */
    void addRequest(int destination, int direction) {
        if (direction == Elevators.UP)
            addUpRequest(destination);
        else
            addDownRequest(destination);
    }

    /**
     * Used for when someone inside the elevator pushes a button
     * @param destination The destination floor.
     */
    void addRequest(int destination) {
        System.out.println("Adding request, destination = " + destination);
        if (this.direction == Elevators.UP) {
            if ((destination - floor) > 0) {
                // Your destination is on your way up...
                addRequest(destination, Elevators.UP);
            } else if ((destination - floor) < 0) {
                // Your destination was passed and is served on the way down
                addRequest(destination, Elevators.DOWN);
            }
        } else if (this.direction == Elevators.DOWN) {
            if ((floor - destination) > 0) {
                // Your destination if on your way down...
                addRequest(destination, Elevators.DOWN);
            } else if ((floor - destination) < 0) {
                // Your destination was passed and is served on the way up
                addRequest(destination, Elevators.UP);
            }
        }
    }

    public double calculateDistance(int targetFloor, int direction) {
        targetFloor *= 100;
        // TODO take direction into consideration
        double sum = 0.0;
        
        // If the elevator is idle, it may as well take requests
        if (this.idle)
            return 0.0;

        // If we're there...
        if (reachedFloor(targetFloor))
            return 0.0;
        // Optimization if we are already on our way there, in the right direction
        if (upQueue.contains(targetFloor) || downQueue.contains(targetFloor))
            return 0.0;

        // If we're going up
        if (this.direction == Elevators.UP) {

            if (direction == Elevators.UP) {

                // They are above us, and wants to go in the same direction
                if (targetFloor - floor > 0) {
                    return 0.0;
                } else { // They are below us and wants to go with us
                    // Calc the cost for going up
                    sum += !upQueue.isEmpty() ? upQueue.getLast() - upQueue.getFirst() : 0;

                    // Cost the cost for going down
                    sum += !downQueue.isEmpty() ? downQueue.getLast() - downQueue.getFirst() : 0;

                    // Ad the last part
//                    sum += Math.abs(downQueue.get(downQueue.size() - 1) - targetFloor);
                }

            } else { // They want to go down
                // Calc the cost for going up
                sum += !upQueue.isEmpty() ? upQueue.getLast() - upQueue.getFirst() : 0;

                // Ad the last part
//                sum += Math.abs(upQueue.get(upQueue.size() - 1) - targetFloor );
            }
        } else { // If we're going down

            if (direction == Elevators.DOWN) {
                // They are below us, and wants to go in the same direction
                if (floor - targetFloor > 0) {
                    return 0.0;
                } else {
                    // Cost the cost for going down
                    sum += !downQueue.isEmpty() ? downQueue.getLast() - downQueue.getFirst() : 0;

                    // Calc the cost for going up
                    sum += !upQueue.isEmpty() ? upQueue.getLast() - upQueue.getFirst() : 0;

                    // Ad the last part
//                    sum += Math.abs(upQueue.get(upQueue.size() - 1) - targetFloor);
                }
            } else {
                // Cost for going down
                sum += !downQueue.isEmpty() ? downQueue.getLast() - downQueue.getFirst() : 0;

                // And then going up
//                sum += Math.abs(downQueue.get(downQueue.size() - 1) - targetFloor);
            }
        }

        System.out.printf("==== DEBUG ===\nElevator #%d calculated the cost for going to %d to: %f\n", id, targetFloor, sum);

        return sum;
    }

    void stop() {
        System.out.println(this);
        printMyQueues();
        System.out.println();
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

    void setFloor(int floor) {
        this.floor = floor;
    }

    @Override
    public String toString() {
        return String.format("Elevator #%d, at floor %d, moving %d, workingQueue: %d", id, floor, direction, workingQueue);
    }

    public void printMyQueues() {
        System.out.println("---> ELEVATOR #"+id);
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

    private boolean reachedFloor(int target) {
//        System.out.printf("target: %d floor: %d\n", target, floor);
        if (target == -1) return false;
        // Test value, may be subject to change TODO!!!
        if (Math.abs(floor - target) <= 6) {
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        // These initialize to different numbers to make if statement work and not spam the elevator with the same cmd
        int target = -1;
        int prevTarget = -2;
        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Check to see if it reached it's floor
            if (reachedFloor(target)) {
                stop();
                toggleDoors();
                if (workingQueue == Elevators.UP)
                    upQueue.removeFirstOccurrence(target);
                if (workingQueue == Elevators.DOWN)
                    downQueue.removeFirstOccurrence(target);
            }

            // If we have no target, we want a new one.
            target = getNextDestination();
            if (target != prevTarget && target != -1) {
                idle = false;
                goTowardsTarget(target);
                prevTarget = target;
            } else if (target == -1) {
                idle = true;
            }
        }
    }

    private int getNextDestination() {
        // We're going up
        if (direction == Elevators.UP) {
            for (Integer integer : upQueue) {
                if (integer > floor) {
                    workingQueue = Elevators.UP;
                    return integer;
                }
            }
            if (downQueue.peek() != null) {
                workingQueue = Elevators.DOWN;
                return downQueue.peek();
            }
        } else {
            // We're going down
            for (Integer integer : downQueue) {
                if (integer < floor) {
                    workingQueue = Elevators.DOWN;
                    return integer;
                }
            }
            if (upQueue.peek() != null) {
                workingQueue = Elevators.UP;
                return upQueue.peek();
            }
        }
        // We have no target, pls give
        return -1;
    }

    private void goTowardsTarget(int target) {
        if (floor - (target) > 0) {
            direction = Elevators.DOWN;
            controller.sendCommand("m " + id + " -1");
        } else {
            direction = Elevators.UP;
            controller.sendCommand("m " + id + " 1");
        }
    }

}