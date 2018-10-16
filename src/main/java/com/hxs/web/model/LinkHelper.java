package com.hxs.web.model;

import com.hxs.data.models.FileMetadata;
import com.hxs.utils.RepoResourceUtils;
import com.hxs.web.controllers.FileController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 *  Hateoas Link Builder Utility Class to help keep Controllers clean
 *
 *  Note: Passing a method name String is not ideal as it is not checked during compile time.
 *       This was the suggested work-around because some controller methods:
 *          - throw a checked Exception that cannot be caught here
 *          - have method parameters that can't be null or instantiated easily; mocking/dummies are heavy handed
 * @author HSteidel
 */

public final class LinkHelper {

    private static final Logger logger = LoggerFactory.getLogger(LinkHelper.class);


    private LinkHelper() {}


    /**
     * Add links to FileMetadata by wrapping it in a Resource
     */
    public static Resource<FileMetadata> generateFileResource(final FileMetadata file) {
        Resource<FileMetadata> resource = new Resource<>(file);
        addFileMetadataLinks(resource);
        return resource;
    }

    /**
     * Adds links to FileMetadata Resources
     */
    private static void addFileMetadataLinks(final Resource<FileMetadata> resource){
        FileMetadata file = resource.getContent();
        resource.add(linkTo(methodOn(FileController.class).serveFile(file.getFilename())).withRel("download"));
        if(RepoResourceUtils.isFileStreamable(file.getFilename())){
            try {
                resource.add(linkTo(FileController.class,
                        FileController.class.getMethod("streamAndSeek", String.class,
                                HttpServletResponse.class, HttpServletRequest.class), file.getFilename())
                        .withRel("play"));
            } catch (NoSuchMethodException e) {
                logger.warn("Could not create 'play' link but we can move on.", e);
            }
        }
    }


    /**
     * Add links to a PagedResource of FileMetadata Resources
     */
    public static PagedResources addLinksToFileMetadataPage(PagedResources<Resource<FileMetadata>> resources){
        resources.forEach(LinkHelper::addFileMetadataLinks);
        return resources;
    }

}
