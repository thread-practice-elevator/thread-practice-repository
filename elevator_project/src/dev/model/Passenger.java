package dev.model;

import java.time.LocalTime;

/**
 * 승객 모델 클래스
 */
public class Passenger {
    private static int idCounter = 1;
    
    private final int id;
    private final int startFloor;
    private final int destinationFloor;
    private final LocalTime requestTime;
    private LocalTime boardingTime;
    private LocalTime arrivalTime;
    
    public Passenger(int startFloor, int destinationFloor) {
        this.id = idCounter++;
        this.startFloor = startFloor;
        this.destinationFloor = destinationFloor;
        this.requestTime = LocalTime.now();
    }
    
    // Getters
    public int getId() {
        return id;
    }
    
    public int getStartFloor() {
        return startFloor;
    }
    
    public int getDestinationFloor() {
        return destinationFloor;
    }
    
    public LocalTime getRequestTime() {
        return requestTime;
    }
    
    public LocalTime getBoardingTime() {
        return boardingTime;
    }
    
    public LocalTime getArrivalTime() {
        return arrivalTime;
    }
    
    // 승객의 이동 방향
    public Direction getDirection() {
        if (destinationFloor > startFloor) {
            return Direction.UP;
        } else if (destinationFloor < startFloor) {
            return Direction.DOWN;
        }
        return Direction.IDLE;
    }
    
    // 시간 기록
    public void board() {
        this.boardingTime = LocalTime.now();
    }
    
    public void arrive() {
        this.arrivalTime = LocalTime.now();
    }
    
    // 대기 시간 계산 (초 단위)
    public long getWaitingTime() {
        if (boardingTime == null) {
            return java.time.Duration.between(requestTime, LocalTime.now()).getSeconds();
        }
        return java.time.Duration.between(requestTime, boardingTime).getSeconds();
    }
    
    // 총 소요 시간 계산 (초 단위)
    public long getTotalTime() {
        if (arrivalTime == null) {
            return getWaitingTime();
        }
        return java.time.Duration.between(requestTime, arrivalTime).getSeconds();
    }
    
    // 탑승 시간 계산 (초 단위)
    public long getRidingTime() {
        if (boardingTime == null || arrivalTime == null) {
            return 0;
        }
        return java.time.Duration.between(boardingTime, arrivalTime).getSeconds();
    }
    
    @Override
    public String toString() {
        return String.format("승객%d[%d→%d, 요청:%s]", 
            id, startFloor, destinationFloor, requestTime.toString().substring(0, 8));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Passenger passenger = (Passenger) obj;
        return id == passenger.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}