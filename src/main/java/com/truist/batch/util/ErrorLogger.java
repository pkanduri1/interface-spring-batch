package com.truist.batch.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorLogger {

    private static final Logger log = LoggerFactory.getLogger(ErrorLogger.class);

    public static void logError(Map<String, Object> item, String sourceSystem, String jobName, String errorMessage, String logFilePath) {
        try (FileWriter fw = new FileWriter(logFilePath, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println("SourceSystem: " + sourceSystem);
            out.println("JobName: " + jobName);
            out.println("Error: " + errorMessage);
            out.println("Row: " + item.toString());
            out.println("----");

        } catch (IOException e) {
            log.error("Failed to write error to log file: {}", logFilePath, e);
        }
    }
}
