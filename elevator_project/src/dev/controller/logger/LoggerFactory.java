package dev.controller.logger;

import javax.swing.JTextArea;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LoggerFactory {
    private JTextArea logArea;
    
    public void setLogArea(JTextArea logArea) {
        this.logArea = logArea;
    }

    public void log(String message) {
        if (logArea != null) {
            logArea.append(message + "\n");
        } else {
            System.out.println(message);
        }
    }
}