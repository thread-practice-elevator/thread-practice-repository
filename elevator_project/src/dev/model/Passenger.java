package dev.model;

public class Passenger {

	private final int userId;
    private final int startFloor;
    private final int destinationFloor;
    private boolean boarded = false;
    
    
    public Passenger(int userId, int start, int dest) {
        this.userId = userId;
        this.startFloor = start;
        this.destinationFloor = dest;
    }

    
    public int getUserId() { return userId; }
    public int getStartFloor() { return startFloor; }
    public int getDestinationFloor() { return destinationFloor; }
    public boolean isBoarded() { return boarded; }
    public void setBoarded(boolean boarded) { this.boarded = boarded; }

}
