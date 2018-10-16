package com.hxs.service.storage.mongo;

import com.hxs.data.models.FileMetadata;
import com.hxs.service.exceptions.ResourceConflictException;
import com.hxs.service.exceptions.ResourceNotFoundException;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import io.vavr.control.Option;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * This service acts as the main service for the store. It uses mongoFileService to perform
 *   CRUD & Query ops. There's no use in interfacing this, this whole app is tightly coupled with MongoDB/GridFS
 */
@Service
public class MongoStorageService {

    private static final Logger logger = LoggerFactory.getLogger(MongoStorageService.class);

    private final MongoFileService mongoFileService;

    public MongoStorageService(MongoFileService mongoFileService) {
        this.mongoFileService = mongoFileService;
    }

    /**
     * Store as a GridFSfile in the collection, otherwise, replace file byte content if exists
     */
    public GridFSDBFile store(String filename, InputStream is) {
        GridFSDBFile storedFile;
        GridFSDBFile existing = mongoFileService.findFileByName(filename);
        if(existing != null){
            storedFile = overwriteExisting(existing, is);
        } else {
            storedFile = saveNewFile(filename, is);
        }
        return storedFile;
    }

    /**
     * Create a new file in the collection
     */
    private GridFSDBFile saveNewFile(String filename, InputStream is){
        GridFSFile newFile = mongoFileService.storeFile(is, filename);
        newFile.put("contentType", new Tika().detect(filename));
        newFile.put(FileMetadata.LAST_MODE_DATE_KEY, Date.from(Instant.now()));
        newFile.save();
        logger.debug("Saved file new file: " + newFile);
        return mongoFileService.findFileByName(filename);
    }

    /**
     *  "Replaces" a file in the collection, it's not directly supported so we have to delete, and create a new one
     *  while preserving any user provided metadata
     */
    private GridFSDBFile overwriteExisting(GridFSDBFile existingFile, InputStream replacementInputStream){
        deleteFileByFilename(existingFile.getFilename());
        saveNewFile(existingFile.getFilename(), replacementInputStream);
        return updateFileMetadata(existingFile.getFilename(), new FileMetadata(existingFile));
    }

    /**
     * Fetches a GridFS file given its name.
     *
     */
    public GridFSDBFile getFileByFilenameOrThrowNotFound(String filename){
        return maybeLoadFileByFilename(filename).getOrElseThrow(() ->
                new ResourceNotFoundException("File not found. Filename: " + filename));
    }


    /**
     * Updates a file's metadata.
     *  - Filename must not be null
     *
     */
    public GridFSDBFile updateFileMetadata(String filename, FileMetadata metadata) {
        GridFSDBFile updatedFile;
        Option<GridFSDBFile> existing = maybeLoadFileByFilename(filename);
        if (existing.isDefined()) {
            updatedFile = existing.get();

            /*Filename duplication check and filename null check*/
            if(!StringUtils.isEmpty(metadata.getFilename()) && !updatedFile.getFilename().equals(metadata.getFilename())) {
                logger.info(filename + " name change request. Checking for duplicate.");
                GridFSDBFile dupe = maybeLoadFileByFilename(metadata.getFilename()).getOrNull();
                if (dupe != null && dupe.getId() != null && dupe.getId() != updatedFile.getId()) {
                    throw new ResourceConflictException("Update file failed due to renaming conflict. " + metadata.getFilename() + " already exists.");
                }
            }

            if(!StringUtils.isEmpty(metadata.getFilename())){
                updatedFile.put("filename", metadata.getFilename());
            }

            updatedFile.setMetaData(metadata.getMetaData());
            updatedFile.put(FileMetadata.LAST_MODE_DATE_KEY, Date.from(Instant.now()));
            updatedFile.put("aliases", metadata.getAliases());
            updatedFile.save();
        } else {
            throw new ResourceNotFoundException("File not found. " + filename);
        }
        return updatedFile;
    }


    private Option<GridFSDBFile> maybeLoadFileByFilename(String filename) {
        return Option.of(mongoFileService.findFileByName(filename));
    }

    /**
     * Fetches all files
     */
    public List<GridFSDBFile> getAllFiles(){
        return mongoFileService.findAllFiles();
    }


    /**
     * Permanently deletes a file
     */
    public void deleteFileByFilename(String filename){
        Option<GridFSDBFile> file = maybeLoadFileByFilename(filename);
        if(file.isDefined()){
            mongoFileService.deleteSingleGridFile(file.get());
        }
    }


    /**
     * Delete a set of files
     */
    public void batchDeleteAListOfFiles(List<String> filenameList){
        filenameList.forEach(this::deleteFileByFilename);
    }
}
