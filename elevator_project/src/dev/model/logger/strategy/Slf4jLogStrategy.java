package dev.model.logger.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jLogStrategy implements LogStrategy {
    private final Logger logger;

    public Slf4jLogStrategy(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    @Override
    public void log(String level, String message) {
        switch (level.toUpperCase()) {
            case "INFO" -> logger.info(message);
            case "WARN" -> logger.warn(message);
            case "ERROR" -> logger.error(message);
            case "DEBUG" -> logger.debug(message);
            default -> logger.info(message);
        }
    }
}
