package com.hxs.functional.base;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.hxs.functional.json.FileTestJson;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static io.restassured.RestAssured.given;

/**
 *
 *  Functional Test Starter class for testing the File Repository Micro-service
 *
 * @author HSteidel
 */
public class FunctionalTest {

    private static ObjectMapper mapper;

    @ClassRule
    public final static TemporaryFolder TEMP_FOLDER = new TemporaryFolder();

    public static final String FILES = "/files";

    private static Properties props = new Properties();

    @BeforeClass
    public static void setupClass() throws Exception {


        InputStream is = ClassLoader.getSystemResourceAsStream("test.properties");
        props.load(is);
        String FILE_REPO_HOST = props.getProperty("store.host", "http://localhost");
        String FILE_REPO_PORT = props.getProperty("store.port", "8080");

        RestAssured.port = Integer.valueOf(FILE_REPO_PORT);

        String baseHost = FILE_REPO_HOST;
        if(baseHost==null){
            baseHost = "http://localhost";
        }
        RestAssured.baseURI = baseHost;

        /*Test server is up, else show disclaimer*/
        try {
            given().when().get(FILES).then().statusCode(HttpStatus.SC_OK);
        } catch (Exception e) {
            printServiceDisclaimer();
            throw e;
        }

        /*Set up mapper*/
        mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
                (aClass, s) -> {
                    FilterProvider filter = new SimpleFilterProvider();
                    mapper.setFilterProvider(filter);
                    return mapper;
                }
        ));

        /*Start fresh*/
        cleanUpFiles();
    }

    @AfterClass
    public static void tearDownClass(){
    }


    @After
    public void tearDown() throws Exception {
        cleanUpFiles();
    }

    /**
     * Cleans up all files
     */
    private static void cleanUpFiles(){
        Response res = given().get(FILES);
        List<FileTestJson> files = getFileMetadataListFromResponse(res);
        files.forEach(f -> {
            given().delete(FILES + "/"+f.getFilename());
        });
    }


    public RequestSpecification givenJsonAcceptingSpec(){
        return given().contentType("application/json");
    }


    public static List<FileTestJson> getFileMetadataListFromResponse(Response res){
        List<FileTestJson> files = new ArrayList<>();
        HashMap emb = res.jsonPath().get("_embedded");
        if( emb != null) {
            List<HashMap<String, String>> list = (List<HashMap<String, String>>) emb.get("fileMetadataList");
            list.forEach(hm -> files.add(mapper.convertValue(hm, FileTestJson.class)));
        }
        return files;
    }


    private static void printServiceDisclaimer(){
        System.out.println("\n\n\n\n");
        System.out.println("*****************************************************");
        System.out.println("This test requires the target service to be running!!");
        System.out.println("Please ensure the service's connection properties are");
        System.out.println("correct or that the service is running.");
        System.out.println("*****************************************************");
    }
}
