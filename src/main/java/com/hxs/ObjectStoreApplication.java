package com.hxs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *  Object Store Service
 *
 *  This service is a little POC for a DIY AWS S3 like application using Mongo's GridFS driver.
 *
 *
 * @author HSteidel
 */
@SpringBootApplication
public class ObjectStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObjectStoreApplication.class, args);
    }

}
