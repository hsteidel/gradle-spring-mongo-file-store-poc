package com.hxs.functional.base;

import com.hxs.functional.json.FileTestJson;
import io.restassured.response.Response;
import org.junit.Assert;

import static org.hamcrest.Matchers.is;

/**
 *  Functional Test with some functional utility methods to help create Operational tests quicker without repeating code
 *
 *  Note: These methods actually call the service and perform some validation.
 *
 * @author HSteidel
 */
public class SystemFunctionalTest extends FunctionalTest {

    public FileTestJson getFileByName(String filename, int expectedStatus){
        Response res = givenJsonAcceptingSpec()
                        .get(FILES+"/"+filename+"/metadata");
        Assert.assertThat(res.statusCode(), is(expectedStatus));
        return res.getBody().as(FileTestJson.class);
    }


}
