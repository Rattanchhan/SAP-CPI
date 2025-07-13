package com.apsaraconsulting.skyvvaadapter.utils;

import org.apache.commons.io.FileUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * @author Ilya Nesterov
 */
public class TestDataLoader {

    public static byte[] loadTestData(String path) throws IOException {
        return FileUtils.readFileToByteArray(Paths.get("src/test/resources", path).toFile());
    }

    public static String loadTestDataAsString(String path) throws IOException {
        return FileUtils.readFileToString(Paths.get("src/test/resources", path).toFile(), StandardCharsets.UTF_8);
    }

    public static FileInputStream loadTestDataAsFileInputStream(String path) throws IOException {
        return new FileInputStream(Paths.get("src/test/resources", path).toFile());
    }

    public static void writeStringToFile(String path, String data) throws IOException {
        FileUtils.writeStringToFile(
            Paths.get("src/test/resources", path).toFile(),
            data,
            StandardCharsets.UTF_8
        );
    }
}
