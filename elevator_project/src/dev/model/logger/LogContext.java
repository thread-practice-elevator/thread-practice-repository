package dev.model.logger;

import dev.model.logger.strategy.LogStrategy;

public class LogContext {
    private static LogStrategy strategy;

    public static void setStrategy(LogStrategy newStrategy) {
        strategy = newStrategy;
    }

    public static void log(String level, String message) {
        if (strategy == null) {
            throw new IllegalStateException("LogStrategy가 설정되지 않았습니다.");
        }
        strategy.log(level, message);
    }
}
