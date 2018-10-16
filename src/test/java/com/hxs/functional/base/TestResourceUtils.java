package com.hxs.functional.base;

import org.junit.Assert;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *  Test helper utils
 * @author HSteidel
 */
public class TestResourceUtils {


    /**
     * Gets a Path given a filename; expects the file to be in the 'resources' tempFolder
     * @param name
     * @return
     */
    public static Path getResourceByName(String name, TemporaryFolder temporaryFolder){
        Path resource = Paths.get("src/test/resources/files/"+name);
        if(resource.toFile().exists()){
            try {
                temporaryFolder.create();
                System.out.println("Using temp folder: " + temporaryFolder.getRoot().getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                Assert.fail("Should not fail on temp folder create.");
            }
            return copyFile(resource, name, temporaryFolder).toPath();
        } else {
            throw new RuntimeException("Could not find: " + name);
        }
    }


    /**
     *  Writes content to a file; Note: overwrites previous content.
     * @param file
     * @param content
     */
    public static void writeToFile(Path file, String content){
        try (BufferedWriter writer = Files.newBufferedWriter(file)){
            writer.write(content);
        } catch (Exception e){
            e.printStackTrace();
            Assert.fail("Failed to write to file.");
        }
    }


    /**
     * Copies a file in the current junit temp folder
     * @param original
     * @param copyName
     * @param temporaryFolder
     * @return
     */
    public static File copyFile(Path original, String copyName, TemporaryFolder temporaryFolder) {
        File copy = null;
        try {
            copy = temporaryFolder.newFile(copyName);
            Files.copy(original, new FileOutputStream(copy));
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Should not fail on file copy ops.");
        }
        return copy;
    }

}
