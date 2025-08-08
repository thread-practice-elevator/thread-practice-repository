package dev.model.logger.strategy;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class FileLogStrategy implements LogStrategy {
    private final PrintWriter writer;

    public FileLogStrategy(String filePath) {
        try {
            writer = new PrintWriter(new FileWriter(filePath, true));
        } catch (IOException e) {
            throw new RuntimeException("로그 파일 생성 실패: " + e.getMessage());
        }
    }

    @Override
    public void log(String level, String message) {
        writer.println(LocalDateTime.now() + " [" + level + "] " + message);
        writer.flush();
    }
}
