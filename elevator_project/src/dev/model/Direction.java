package dev.model;

/**
 * 엘레베이터의 이동 방향을 나타내는 열거형
 */
public enum Direction {
    UP("위로"),
    DOWN("아래로"),
    IDLE("정지");
    
    private final String description;
    
    Direction(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}