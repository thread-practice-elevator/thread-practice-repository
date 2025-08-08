package dev.view.logger;

import dev.model.logger.strategy.LogStrategy;

public class ConsoleLogStrategy implements LogStrategy {
    @Override
    public void log(String level, String message) {
        System.out.println("[" + level + "] " + message);
    }
}
