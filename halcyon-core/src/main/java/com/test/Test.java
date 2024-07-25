package com.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author erich
 */
public class Test {

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) throws IOException {
        File file = new File("/mnt/d/projects/Halcyon/Halcyon/settings.ttl");
        BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        Instant instant = attrs.lastModifiedTime().toInstant();
        String isoDateTime = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC")).format(instant);
        System.out.println(isoDateTime);
    }
}
