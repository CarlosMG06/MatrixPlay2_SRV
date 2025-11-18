package com.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

public class UtilsLog {

    private static final Path logPath = Path.of("Matrixplay2.log");

    public static synchronized void write(String text) {
        try {
            Files.writeString(logPath, text, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void info(String text){
        String line = LocalDateTime.now() + " - " + text + "\n";
        write("[INFO] "+line);

    }

    public static synchronized void warning(String text){
        String line = LocalDateTime.now() + " - " + text + "\n";
        write("[WARNING] "+line);

    }

}