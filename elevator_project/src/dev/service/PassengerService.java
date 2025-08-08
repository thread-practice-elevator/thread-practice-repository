package dev.service;

import dev.model.Direction;
import dev.model.Passenger;
import dev.controller.logger.LoggerFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PassengerService {
    private final Queue<Passenger> waitingPassengers;
    private final List<Passenger> completedPassengers;
    private final LoggerFactory loggerFactory;
    
    public PassengerService(LoggerFactory loggerFactory) {
        this.waitingPassengers = new ConcurrentLinkedQueue<>();
        this.completedPassengers = new ArrayList<>();
        this.loggerFactory = loggerFactory;
    }
    
    /**
     * 승객 객체 생성 및 대기열에 추가 (기존 메서드)
     */
    public void addPassengerRequest(int startFloor, int destinationFloor) {
        Passenger passenger = new Passenger(startFloor, destinationFloor);
        waitingPassengers.offer(passenger);
        loggerFactory.info("승객 요청 추가: " + passenger);
    }
    
    /**
     * 외부에서 생성된 승객 객체를 대기열에 추가하는 새로운 메서드
     */
    public void addPassengerRequest(Passenger passenger) {
        waitingPassengers.offer(passenger);
        loggerFactory.info("승객 객체 대기열에 추가: " + passenger);
    }
    
    /**
     * 현재 층에서 하차하는 승객 리스트 반환
     */
    public List<Passenger> getExitingPassengers(List<Passenger> onboardPassengers, int currentFloor) {
        return onboardPassengers.stream()
            .filter(p -> p.getDestinationFloor() == currentFloor)
            .toList();
    }
    
    /**
     * 하차 처리
     */
    public void processPassengerExit(Passenger passenger) {
        passenger.arrive();
        completedPassengers.add(passenger);
        loggerFactory.info("  " + passenger + " 하차 (소요시간: " + passenger.getTotalTime() + "초)");
    }
    
    /**
     * 현재 층에서 탑승 가능한 승객 리스트 반환
     */
    public List<Passenger> getBoardingPassengers(int currentFloor, Direction elevatorDirection, int availableCapacity) {
        List<Passenger> boardingPassengers = new ArrayList<>();
        Iterator<Passenger> iterator = waitingPassengers.iterator();
        
        while (iterator.hasNext() && boardingPassengers.size() < availableCapacity) {
            Passenger passenger = iterator.next();
            
            if (passenger.getStartFloor() == currentFloor) {
                // 승객의 방향이 엘리베이터 방향과 일치하거나 엘리베이터가 정지 상태인 경우 탑승
                if (elevatorDirection == Direction.IDLE || passenger.getDirection() == elevatorDirection) {
                    boardingPassengers.add(passenger);
                    iterator.remove();
                }
            }
        }
        return boardingPassengers;
    }
    
    /**
     * 탑승 처리
     */
    public void processPassengerBoarding(Passenger passenger) {
        passenger.board();
        loggerFactory.info("  " + passenger + " 탑승 (대기시간: " + passenger.getWaitingTime() + "초)");
    }
    
    /**
     * 모든 대기 승객의 출발 층을 요청 리스트로 반환
     */
    public List<Integer> getWaitingPassengerFloors() {
        return waitingPassengers.stream()
            .map(Passenger::getStartFloor)
            .toList();
    }
    
    public boolean hasWaitingPassengers() {
        return !waitingPassengers.isEmpty();
    }

    // Getters
    public Queue<Passenger> getWaitingPassengers() {
        return new ConcurrentLinkedQueue<>(waitingPassengers);
    }
    
    public List<Passenger> getCompletedPassengers() {
        return new ArrayList<>(completedPassengers);
    }
    
    public int getWaitingPassengerCount() {
        return waitingPassengers.size();
    }
}