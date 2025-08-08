package dev.model;

import java.util.*;

/**
 * 엘레베이터 모델 클래스
 * SCAN 알고리즘을 위한 핵심 데이터를 관리합니다.
 * DefaultElevator 인터페이스를 구현합니다.
 */
public class Elevator implements DefaultElevator {
    private final String id;                    // 엘레베이터 ID
    private int currentFloor;                   // 현재 층
    private Direction direction;                // 현재 방향
    private final int minFloor;                 // 최저층
    private final int maxFloor;                 // 최고층
    private final Set<Integer> requests;        // 요청된 층들
    private final Queue<Passenger> passengers;  // 탑승한 승객들
    private final int capacity;                 // 최대 수용 인원
    private boolean isMoving;                   // 이동 중 여부
    
    public Elevator(int minFloor, int maxFloor, int capacity) {
        this("ELV-DEFAULT", minFloor, maxFloor, capacity);
    }
    
    public Elevator(String id, int minFloor, int maxFloor, int capacity) {
        this.id = id;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.capacity = capacity;
        this.currentFloor = minFloor;
        this.direction = Direction.IDLE;
        this.requests = new TreeSet<>();
        this.passengers = new LinkedList<>();
        this.isMoving = false;
    }
    
    // ========== DefaultElevator 인터페이스 구현 ==========
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public int getMinFloor() {
        return minFloor;
    }
    
    @Override
    public int getMaxFloor() {
        return maxFloor;
    }
    
    @Override
    public int getCapacity() {
        return capacity;
    }
    
    @Override
    public int getCurrentFloor() {
        return currentFloor;
    }
    
    @Override
    public Direction getCurrentDirection() {
        return direction;
    }
    
    @Override
    public int getCurrentPassengerCount() {
        return passengers.size();
    }
    
    @Override
    public boolean isFull() {
        return passengers.size() >= capacity;
    }
    
    @Override
    public boolean isEmpty() {
        return passengers.isEmpty();
    }
    
    @Override
    public boolean isMoving() {
        return isMoving;
    }
    
    @Override
    public boolean setCurrentFloor(int floor) {
        if (isValidFloor(floor)) {
            this.currentFloor = floor;
            return true;
        }
        return false;
    }
    
    @Override
    public void setDirection(Direction direction) {
        this.direction = direction;
    }
    
    @Override
    public void addRequest(int floor) {
        if (isValidFloor(floor) && floor != currentFloor) {
            requests.add(floor);
        }
    }
    
    @Override
    public void removeRequest(int floor) {
        requests.remove(floor);
    }
    
    @Override
    public boolean hasRequestAt(int floor) {
        return requests.contains(floor);
    }
    
    @Override
    public boolean hasRequests() {
        return !requests.isEmpty();
    }
    
    @Override
    public Set<Integer> getRequests() {
        return new TreeSet<>(requests);
    }
    
    @Override
    public Integer getNextDestination() {
        return getNextFloor(); // 기존 메서드 재사용
    }
    
    @Override
    public boolean addPassenger(Passenger passenger) {
        if (!isFull() && passenger.getStartFloor() == currentFloor) {
            passengers.offer(passenger);
            addRequest(passenger.getDestinationFloor());
            return true;
        }
        return false;
    }
    
    @Override
    public List<Passenger> removePassengersAt(int floor) {
        List<Passenger> removedPassengers = new ArrayList<>();
        Iterator<Passenger> iterator = passengers.iterator();
        
        while (iterator.hasNext()) {
            Passenger passenger = iterator.next();
            if (passenger.getDestinationFloor() == floor) {
                removedPassengers.add(passenger);
                iterator.remove();
            }
        }
        
        return removedPassengers;
    }
    
    @Override
    public List<Passenger> getCurrentPassengers() {
        return new ArrayList<>(passengers);
    }
    
    @Override
    public boolean canMoveTo(int floor) {
        return isValidFloor(floor) && floor != currentFloor && !isMoving;
    }
    
    @Override
    public boolean isAtBoundary() {
        return currentFloor == minFloor || currentFloor == maxFloor;
    }
    
    @Override
    public boolean isValidFloor(int floor) {
        return floor >= minFloor && floor <= maxFloor;
    }
    
    @Override
    public void reset() {
        currentFloor = minFloor;
        direction = Direction.IDLE;
        isMoving = false;
        requests.clear();
        passengers.clear();
    }
    
    @Override
    public void stop() {
        direction = Direction.IDLE;
        isMoving = false;
    }
    
    // ========== 기존 메서드들 (정리 및 유지) ==========
    
    /**
     * 기존 방식의 승객 목록 조회 (하위 호환성)
     */
    public List<Passenger> getPassengers() {
        return getCurrentPassengers();
    }
    
    /**
     * 기존 방식의 승객 제거 (하위 호환성)
     */
    public void removePassengersAt_Old(int floor) {
        passengers.removeIf(p -> p.getDestinationFloor() == floor);
    }
    
    /**
     * 기존 방식의 방향 조회 (하위 호환성)
     */
    public Direction getDirection() {
        return getCurrentDirection();
    }
    
    // ========== SCAN 알고리즘 핵심 로직 ==========
    
    /**
     * SCAN 알고리즘을 위한 다음 층 결정
     */
    public Integer getNextFloor() {
        if (requests.isEmpty()) {
            return null;
        }
        
        return switch (direction) {
            case UP -> getNextUpwardFloor();
            case DOWN -> getNextDownwardFloor();
            default -> getClosestFloor();
        };
    }
    
    private Integer getNextUpwardFloor() {
        // 현재 층보다 위에 있는 요청 중 가장 가까운 층
        for (Integer floor : requests) {
            if (floor > currentFloor) {
                return floor;
            }
        }
        
        // 위쪽에 요청이 없으면 방향을 바꿔서 아래쪽 요청 처리
        if (hasDownwardRequests()) {
            setDirection(Direction.DOWN);
            return getNextDownwardFloor();
        }
        
        return null;
    }
    
    private Integer getNextDownwardFloor() {
        // 현재 층보다 아래에 있는 요청 중 가장 가까운 층 (내림차순으로)
        Integer[] floorsArray = requests.toArray(new Integer[0]);
        for (int i = floorsArray.length - 1; i >= 0; i--) {
            if (floorsArray[i] < currentFloor) {
                return floorsArray[i];
            }
        }
        
        // 아래쪽에 요청이 없으면 방향을 바꿔서 위쪽 요청 처리
        if (hasUpwardRequests()) {
            setDirection(Direction.UP);
            return getNextUpwardFloor();
        }
        
        return null;
    }
    
    private Integer getClosestFloor() {
        // 방향이 IDLE일 때는 가장 가까운 층으로
        Integer closest = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (Integer floor : requests) {
            int distance = Math.abs(floor - currentFloor);
            if (distance < minDistance) {
                minDistance = distance;
                closest = floor;
            }
        }
        
        // 방향 설정
        if (closest != null) {
            if (closest > currentFloor) {
                setDirection(Direction.UP);
            } else if (closest < currentFloor) {
                setDirection(Direction.DOWN);
            }
        }
        
        return closest;
    }
    
    private boolean hasUpwardRequests() {
        return requests.stream().anyMatch(floor -> floor > currentFloor);
    }
    
    private boolean hasDownwardRequests() {
        return requests.stream().anyMatch(floor -> floor < currentFloor);
    }
    
    // ========== 유틸리티 메서드 ==========
    
    /**
     * 이동 상태 설정
     */
    public void setMoving(boolean moving) {
        this.isMoving = moving;
    }
    
    @Override
    public String toString() {
        return String.format("%s[층:%d, 방향:%s, 요청:%s, 승객:%d/%d, 이동중:%s]", 
            id, currentFloor, direction, requests, passengers.size(), capacity, 
            isMoving ? "예" : "아니오");
    }
}