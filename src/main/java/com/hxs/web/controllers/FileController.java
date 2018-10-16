package com.hxs.web.controllers;

import com.hxs.data.models.FileMetadata;
import io.swagger.annotations.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

/**
 * @author HSteidel
 */
@Api(
        tags = "Files",
        value = "/files",
        description = "File operations."
)
@RequestMapping("/files")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface FileController {

    @ApiOperation(
            value = "Upload a file into the system",
            notes = "Single endpoint to upload files.",
            response = FileMetadata.class,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ApiImplicitParams(value = {
            @ApiImplicitParam(
                    name = "file",
                    value = "File to upload",
                    required = true,
                    dataType = "file",
                    paramType = "form")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "File upload success"),
            @ApiResponse(code = 403, message = "Operation forbidden"),
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Resource<FileMetadata> handleFileUpload(HttpServletRequest request) throws Exception;


    @ApiOperation(
            value = "Get a list of all files in the system",
            response = FileMetadata.class,
            responseContainer = "List",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiImplicitParams(value = {
            @ApiImplicitParam(
                    name = "pageable",
                    value = "Pageable query options",
                    required = false,
                    paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "File(s) found"),
    })
    @GetMapping
    PagedResources<Resource<FileMetadata>> getAllFiles(Pageable pageable, PagedResourcesAssembler assembler);


    @ApiOperation(
            value = "Get the contents of a specific file in the system",
            notes = "Gets the contents of a file in the system with the specified filename. ",
            response = MultipartFile.class,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    @ApiImplicitParams(value = {
            @ApiImplicitParam(
                    name = "filename",
                    value = "The filename of the file to fetch",
                    required = true,
                    dataType = "String",
                    paramType = "path")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "File found"),
            @ApiResponse(code = 404, message = "File not found")
    })
    @GetMapping("/{filename:.+}/content")
    ResponseEntity<Resource> serveFile(@PathVariable("filename") String filename);



    @ApiOperation(
            value = "Stream the contents of a specific file in the system",
            notes = "Streams the contents of a file in the system with the specified filename. " +
                    " Streaming supports seeking for audio and video. ",
            response = MultipartFile.class,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    @ApiImplicitParams(value = {
            @ApiImplicitParam(
                    name = "filename",
                    value = "The filename of the file to fetch",
                    required = true,
                    dataType = "String",
                    paramType = "path")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "File found"),
            @ApiResponse(code = 404, message = "File not found")
    })
    @GetMapping("/{filename:.+}/stream")
    void streamAndSeek(@PathVariable("filename") String filename, HttpServletResponse response, HttpServletRequest request) throws Exception;




    @ApiOperation(
            value = "Update the metadata of a file in the system",
            notes = "Updates the metadata for a file in the system with the specified filename given the metadata. ",
            response = FileMetadata.class,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiImplicitParams(value = {
            @ApiImplicitParam(
                    name = "filename",
                    value = "The filename of the file to fetch",
                    required = true,
                    dataType = "String",
                    paramType = "path")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "File update success"),
            @ApiResponse(code = 400, message = "Invalid field(s)"),
            @ApiResponse(code = 404, message = "File does not exist"),
            @ApiResponse(code = 409, message = "Conflict; i.e. file rename is not unique")
    })
    @GetMapping("/{filename:.+}/metadata")
    Resource<FileMetadata> getFileMetadata(@PathVariable("filename") String filename);



    @ApiOperation(
            value = "Update the metadata of a file in the system",
            notes = "Updates the metadata for a file in the system with the specified filename given the metadata. ",
            response = FileMetadata.class,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiImplicitParams(value = {
            @ApiImplicitParam(
                    name = "filename",
                    value = "The filename of the file to fetch",
                    required = true,
                    dataType = "String",
                    paramType = "path"),
            @ApiImplicitParam(
                    name = "metadata",
                    value = "File metadata",
                    required = true,
                    dataType = "FileMetadata",
                    paramType = "body")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "File update success"),
            @ApiResponse(code = 400, message = "Invalid field(s)"),
            @ApiResponse(code = 404, message = "File does not exist"),
            @ApiResponse(code = 409, message = "Conflict; i.e. file rename is not unique")
    })
    @PutMapping("/{filename:.+}/metadata")
    Resource<FileMetadata> updateFileObject(@PathVariable("filename") String filename, @Valid @RequestBody FileMetadata metadata);


    @ApiOperation(
            value = "Partially update the metadata of a file in the system",
            notes = "Updates the metadata for a file in the system with the specified filename given partial metadata. ",
            response = FileMetadata.class,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiImplicitParams(value = {
            @ApiImplicitParam(
                    name = "filename",
                    value = "The filename of the file to fetch",
                    required = true,
                    dataType = "String",
                    paramType = "path"),
            @ApiImplicitParam(
                    name = "metadata",
                    value = "Partial file metadata",
                    required = true,
                    dataType = "FileMetadata",
                    paramType = "body")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "File update success"),
            @ApiResponse(code = 400, message = "Invalid field(s)"),
            @ApiResponse(code = 404, message = "File does not exist"),
            @ApiResponse(code = 409, message = "Conflict; i.e. file rename is not unique")
    })
    @PatchMapping("/{filename:.+}/metadata")
    ResponseEntity<?> partiallyUpdateFileObject(@PathVariable("filename") String filename, @RequestBody FileMetadata metadata);



    @ApiOperation(
            value = "Delete a file",
            notes = "WARNING: Deletes a file permanently. ",
            response = Void.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiImplicitParams(value = {
            @ApiImplicitParam(
                    name = "filename",
                    value = "The filename of the file to delete",
                    required = true,
                    dataType = "String",
                    paramType = "path")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "File delete success"),
            @ApiResponse(code = 404, message = "File does not exist")
    })
    @DeleteMapping("/{filename:.+}/delete")
    void deleteFile(@PathVariable("filename") String filename);



    @ApiOperation(
            value = "Delete a list of files",
            notes = "Deletes a list of files given the filenames. " +
                    " If a particular file is not found, the service will continue" +
                    " to process what it can.",
            response = Void.class,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiImplicitParams(value = {
            @ApiImplicitParam(
                    name = "filenames",
                    value = "The list of filenames of the files to delete",
                    required = true,
                    dataType = "array[string]",
                    paramType = "body")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Multi delete success"),
            @ApiResponse(code = 400, message = "Malformed or empty list")
    })
    @DeleteMapping("/batch")
    void batchDeleteFiles(@RequestBody List<String> filenames);
}
