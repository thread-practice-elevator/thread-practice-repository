package dev.model;

public class Elevator {
	private int currentFloor; // 현재 층
	final int MAX_FLOOR = 10; // 최고 층

	public Elevator(int startFloor) {
		this.currentFloor = startFloor;
	}

	public int getCurrentFloor() {
		return currentFloor;
	}

	public int setCurrentFloor(int floor) {
		return currentFloor = floor;
	}
}
