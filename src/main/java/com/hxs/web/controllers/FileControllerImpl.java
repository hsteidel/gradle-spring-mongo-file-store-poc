package com.hxs.web.controllers;

import com.hxs.data.models.FileMetadata;
import com.hxs.service.exceptions.ResourceNotFoundException;
import com.hxs.service.storage.mongo.MongoStorageService;
import com.hxs.web.download.MultiPartFileSender;
import com.hxs.web.model.LinkHelper;
import com.mongodb.gridfs.GridFSDBFile;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping("/files")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class FileControllerImpl implements FileController{

    private final MongoStorageService storageService;

    @Autowired
    public FileControllerImpl(MongoStorageService storageService) {
        this.storageService = storageService;
    }


    /**
     * Upload a file to the data store.
     *
     * curl -F "file=@C:\data\testupload-1.txt" https://localhost:8080/api/files -k
     *
     * @param request The upload request
     * @return The file as a resource
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Resource<FileMetadata> handleFileUpload(HttpServletRequest request) throws Exception {

        ServletFileUpload upload = new ServletFileUpload();

        GridFSDBFile file = null;

        FileItemIterator items = upload.getItemIterator(request);
        
        while (items.hasNext()) {
            FileItemStream item = items.next();
            file = storageService.store(item.getName(), item.openStream());
        }
        return LinkHelper.generateFileResource(new FileMetadata(file));
    }

    /**
     * Gets a full listing of files in the database
     * @param pageable page query param object
     * @param assembler Page resource builder
     * @return Page of FileMetadata objects
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PagedResources<Resource<FileMetadata>> getAllFiles(Pageable pageable, PagedResourcesAssembler assembler){
        List<GridFSDBFile> files = storageService.getAllFiles();
        List<FileMetadata> metaList = new ArrayList<>();
        files.forEach(file -> metaList.add(new FileMetadata(file)));
        Page<FileMetadata> page = new PageImpl<>(metaList, pageable, metaList.size());
        Link selfLinkWithQueryArgs = linkTo(methodOn(this.getClass()).getAllFiles(pageable, assembler)).withSelfRel();
        PagedResources<Resource<FileMetadata>> resources = assembler.toResource(page, selfLinkWithQueryArgs);
        return LinkHelper.addLinksToFileMetadataPage(resources);
    }

    /**
     * Serve up a file from the data store for download.
     *
     * @param filename identifier of the file
     * @return The file content as a resource
     */
    @GetMapping("/{filename:.+}/content")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity serveFile(@PathVariable("filename") String filename) {
        return serveFile(storageService.getFileByFilenameOrThrowNotFound(filename));
    }
    
    
    
    /**
     * Helper method that actually does the work of serving up a file once we have a GridFSDBFile reference.
     */
    private ResponseEntity serveFile(GridFSDBFile file) {
        if (file == null) {
            throw new ResourceNotFoundException("No file found ");
        }

        return ResponseEntity
                .ok()
                .contentLength(file.getLength())
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(new InputStreamResource(file.getInputStream()));
    }

    /**
     * This method stream's a file from Mongo down to the caller but supports
     * seeking for audio and video.
     *
     * ADD JIRA REF
     *
     * See the magical MultiPartFileSender for details.
     *
     * @param filename identifier of the file
     * @param response The response object
     * @param request The request object
     * @throws Exception on failure
     */
    @GetMapping(path = "/{filename:.+}/stream")
    @ResponseStatus(HttpStatus.OK)
    public void streamAndSeek(@PathVariable("filename") String filename,
                              HttpServletResponse response, HttpServletRequest request) throws Exception {
        GridFSDBFile file = storageService.getFileByFilenameOrThrowNotFound(filename);
        MultiPartFileSender.fromGridFSDBFile(file).with(request).with(response).serveContent();
    }


    @GetMapping("/{filename:.+}/metadata")
    @ResponseStatus(HttpStatus.OK)
    public Resource<FileMetadata> getFileMetadata(@PathVariable("filename") String filename){
        GridFSDBFile file = storageService.getFileByFilenameOrThrowNotFound(filename);
        return LinkHelper.generateFileResource(new FileMetadata(file));
    }


    /**
     * Updates the metadata of the file with the specified filename.
     *
     * @param filename identifier of the file
     * @return The FileMetadata as a response entity
     */
    @PutMapping("/{filename:.+}/metadata")
    @ResponseStatus(HttpStatus.OK)
    public Resource<FileMetadata> updateFileObject(@PathVariable("filename") String filename,
                                                   @Valid @RequestBody FileMetadata metadata) {
        GridFSDBFile file = storageService.updateFileMetadata(filename, metadata);
        return LinkHelper.generateFileResource(new FileMetadata(file));
    }

    /**
     * Partially updates the metadata of the file with the specified filename.
     * Note: any update-able property that is null will be saved as null
     * @param filename filename identifier of the file
     * @return The FileMetadata as a response entity
     */
    @PatchMapping("/{filename:.+}/metadata")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> partiallyUpdateFileObject(@PathVariable("filename") String filename,
                                                       @RequestBody FileMetadata metadata) {
        GridFSDBFile file = storageService.updateFileMetadata(filename, metadata);
        return new ResponseEntity<>(LinkHelper.generateFileResource(new FileMetadata(file)), HttpStatus.OK);
    }

    /**
     * Delete a file and all of its versions
     * @param filename of file to delete
     * @return
     */
    @DeleteMapping("/{filename:.+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(@PathVariable("filename") String filename){
        storageService.deleteFileByFilename(filename);
    }


    @DeleteMapping("/batch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void batchDeleteFiles(@RequestBody List<String> filenames){
        if(filenames == null || filenames.isEmpty()){
            throw new IllegalArgumentException("Invalid file batch list.");
        }
        storageService.batchDeleteAListOfFiles(filenames);
    }
}
