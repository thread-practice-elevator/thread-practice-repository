package dev.model;

public enum Direction {
    UP,
    DOWN,
    IDLE;

    public static Direction setDirection(int source, int destination) {
        if (source > destination) {
            return DOWN;
        } else if (source < destination) {
            return UP;
        } else {
            return IDLE;
        }
    }
}