package com.hxs.functional;

import com.hxs.functional.base.SystemFunctionalTest;
import com.hxs.functional.json.FileTestJson;
import io.restassured.response.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.hxs.functional.base.TestResourceUtils.getResourceByName;
import static com.hxs.functional.base.TestResourceUtils.writeToFile;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * File operation tests on the /files resource
 */
public class FileAPITests extends SystemFunctionalTest {


    /**
     * Test the basic operations
     */
    @Test
    public void testFileBasics() {

        Path uploadFile = getResourceByName("text.txt", TEMP_FOLDER);
        writeToFile(uploadFile, "Content");

        /*
            Upload a file
         */
        Response res = given()
                .multiPart(uploadFile.toFile())
                .when().post(FILES);

        Assert.assertThat(res.statusCode(), is(HttpStatus.SC_CREATED));


        FileTestJson fileJson = res.getBody().as(FileTestJson.class);

        String filename = fileJson.getFilename();
        Assert.assertFalse(StringUtils.isEmpty(filename));
        Assert.assertThat(fileJson.getLength(), is(uploadFile.toFile().length()));
        Assert.assertThat(fileJson.getContentType(), is("text/plain"));


        /*
            Overwrite a file
         */
        writeToFile(uploadFile, "Content changed");

        String originalId = fileJson.getId();
        res = given()
                .multiPart(uploadFile.toFile())
                .when().post(FILES);

        fileJson = res.getBody().as(FileTestJson.class);
        /*Check for overwrite and integrity*/
        Assert.assertThat(res.statusCode(), is(HttpStatus.SC_CREATED));
        Assert.assertThat(fileJson.getLength(), is(uploadFile.toFile().length()));
        Assert.assertThat(fileJson.getId(), not(originalId));
        Assert.assertThat(fileJson.getFilename(), is(filename));


        /*
            Update a file
         */
        List<String> tags = Arrays.asList("Bullet", "Badge", "Gun");
        /*Update tags*/
        fileJson.setAliases(tags);
        res = givenJsonAcceptingSpec().body(fileJson).put(FILES + "/" + filename + "/metadata");
        /*Re-fetch and ensure update*/
        fileJson = res.getBody().as(FileTestJson.class);
        Assert.assertThat(res.statusCode(), is(HttpStatus.SC_OK));
        Assert.assertThat(fileJson.getAliases(), is(tags));
        Assert.assertThat(fileJson.getFilename(), is(filename));


        /*
            Download a file and ensure size
         */
        InputStream inputStream = givenJsonAcceptingSpec().get(FILES + "/" + filename + "/content").asInputStream();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            IOUtils.copy(inputStream, byteArrayOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Should not fial on byte copy");
        }
        IOUtils.closeQuietly(byteArrayOutputStream);
        IOUtils.closeQuietly(inputStream);
        Assert.assertThat((long) byteArrayOutputStream.size(), equalTo(uploadFile.toFile().length()));


        /*
           "Stream" a file given a filename
         */
        inputStream = givenJsonAcceptingSpec().get(FILES + "/" + filename  + "/stream").asInputStream();
        byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            IOUtils.copy(inputStream, byteArrayOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Should not fiale on byte copy");
        }
        IOUtils.closeQuietly(byteArrayOutputStream);
        IOUtils.closeQuietly(inputStream);
        Assert.assertThat((long) byteArrayOutputStream.size(), equalTo(uploadFile.toFile().length()));

        /*
            Delete a file
         */
        res = givenJsonAcceptingSpec().delete(FILES + "/" + filename);
        Assert.assertThat(res.statusCode(), is(HttpStatus.SC_NO_CONTENT));
        /*Then ensure it can't be retrieved*/
        res = givenJsonAcceptingSpec().get(FILES + "/" + filename + "/metadata");
        Assert.assertThat(res.statusCode(), is(HttpStatus.SC_NOT_FOUND));
    }
}