# Object Store

## Overview
This small poc project demonstrates how to use Mongo GridFS over a Spring REST API. 

### Key Features
* Ability to upload, download, and delete files
* Hateoas links will provide optional download links and a "play" link if the file is "streamable"
* Ability to add tags to a file
* Ability to add user defined JSON metadata to go along with the files
* Ability to overwrite files while keeping tags and metadata
* Swagger UI
* Functional JUnit 4 testing

### Future enhancements
The final goal would be to make this more like a true hybrid filesystem:

* Add a front-end for it

* Add ability to create and delete buckets where by a bucket is a separate Mongo collection

* Add ability to version files instead of overwriting them by using a unique identifier and a version property
 - Ability to delete old versions
 - Ability to not update old versions (freeze old versions)
 - Ability to rev back by deleting versions that come after the target version (or maybe push previous version up as the latest)

* Add ability to create, update, and delete folders with the usage of materialized paths to simulate a folder tree structure

* Add ability to move folders or just their content within the folder tree while 
 - not allowing duplicate folders on the same target folder
 - not allowing duplicate files (by filename) on the same target folder
 - all versioned files move along as well
 
* Add ability to move content between buckets while
 - auto-creating full folder paths if the destination bucket doesn't have it
 - bringing along all the file versions
 
* Add ability to auto-create full folder path on file upload

* Add ability to specify a target folder on file upload
 

## Tech
Major technology components:
* Java 8
* Mongo 3.4
* Spring Web / REST & Hateoas
* PMD 6

## Requirements
* Mongo 3.4+
* Java 8+


# Getting Started
These instructions will get you up and running on your local machine.

1. Pull the repository in and open it up with your favorite IDE.
2. Install or setup Mongo 3.4
  -  I highly suggest using Docker since it's quick!
   ```
     docker run --name object-store-mongo -p27017:27017 mongo:3.4
   ```
   - Note: If you don't want to keep it, just run it in interactive and "self-destruct on quit mode" by adding -it --rm to the docker run command
3. If you need to, use your IDE's "run configuration" to overwrite any properties via environment variables
4. Run ObjectStoreApplication.java
5. Open a browser and goto http://localhost:8080 for the Swagger UI
