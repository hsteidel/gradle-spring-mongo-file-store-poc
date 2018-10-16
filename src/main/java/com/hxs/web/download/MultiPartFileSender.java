package com.hxs.web.download;

import com.mongodb.gridfs.GridFSDBFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by kevin on 10/02/15.
 * See full code here : https://github.com/davinkevin/Podcast-Server/blob/d927d9b8cb9ea1268af74316cd20b7192ca92da7/src/main/java/lan/dk/podcastserver/utils/multipart/MultipartFileSender.java
 *
 * Notes:
 *
 * - Spring used to support this functionality out of the box, they dropped it and might bring in back in Spring 5
 *          https://jira.spring.io/browse/SPR-10805
 *
 * - This class is NOT BASED on Kevin's latest version. It is based from an older version he posted 2yrs ago but
 *   people had success so I went with it. We could move to the latest if we see any benefits (perhaps performance).
 *   Here's the version I worked with:
 *          https://gist.github.com/davinkevin/b97e39d7ce89198774b4
 *
 * - Again, this is working and should be used with out modifications:
 *     Ultimately, it only cares about a few fields and that it
 *     has an InputStream to work with. (See "serveContent()" and line 270)
 *
 * - This relies on Apache Tika. Tika helps to determine what mimetype the file in question is. Originally, Kevin created a "Tika/MimeType"
 *    utility class. I'm just using the basic, "guess the mimetype by filename" API, for a quick and dirty.
 *
 *  - I like the builder pattern used here so I expanded on it.
 */
@SuppressWarnings("PMD")
public class MultiPartFileSender {

    private final Logger logger = LoggerFactory.getLogger(MultiPartFileSender.class);

    private static final int DEFAULT_BUFFER_SIZE = 20480; // ..bytes = 20KB.

    private static final long DEFAULT_EXPIRE_TIME = 604800000L; // ..ms = 1 week.

    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";


    private Path filepath;

    private GridFSDBFile gridFSDBFile;

    private boolean useFile;

    private HttpServletRequest request;

    private HttpServletResponse response;

    public static MultiPartFileSender fromGridFSDBFile(GridFSDBFile gridFSDBFile){
        return new MultiPartFileSender().setGridFSDBFile(gridFSDBFile);
    }

    //** internal setter **//
    private MultiPartFileSender setFilepath(Path filepath) {
        this.useFile = true;
        this.filepath = filepath;
        return this;
    }

    /**
     * New addition for GridFS compatability
     * @param gridFSDBFile
     * @return
     */
    private MultiPartFileSender setGridFSDBFile(GridFSDBFile gridFSDBFile){
        this.useFile = false;
        this.gridFSDBFile = gridFSDBFile;
        return  this;
    }

    public MultiPartFileSender with(HttpServletRequest httpRequest) {
        request = httpRequest;
        return this;
    }

    public MultiPartFileSender with(HttpServletResponse httpResponse) {
        response = httpResponse;
        return this;
    }

    /**
     * Main entry point to get the content based on configuration
     * I basically pulled this out of the original serveResource(..) because I felt like that method
     * did too much.
     *
     * "An engine should not have to fetch its own gas, it should be injected with gas"
     *
     * @throws Exception
     */
    public void serveContent() throws Exception{
        if (response == null || request == null) {
            return;
        }

        Long length;
        String fileName ;
        Object lastModifiedObj;
        long lastModified;
        if(useFile) {
            /*ORIGINAL CODE*/
            if (!Files.exists(filepath)) {
                logger.error("File doesn't exist at URI : {}", filepath.toAbsolutePath().toString());
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            length = Files.size(filepath);
            fileName = filepath.getFileName().toString();
            lastModifiedObj = Files.getLastModifiedTime(filepath);
            lastModified = LocalDateTime.ofInstant(((FileTime)lastModifiedObj).toInstant(), ZoneId.of(ZoneOffset.systemDefault().getId())).toEpochSecond(ZoneOffset.UTC);
        } else {
            /*GRIDFS Addition*/
            length = gridFSDBFile.getLength();
            fileName = gridFSDBFile.getFilename();
            lastModifiedObj = gridFSDBFile.getUploadDate();
            lastModified = LocalDateTime.ofInstant(((Date) lastModifiedObj).toInstant(), ZoneId.of(ZoneOffset.systemDefault().getId())).toEpochSecond(ZoneOffset.UTC);
        }


        if (StringUtils.isEmpty(fileName) || lastModifiedObj == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        /*
           Tika basic functionality replacement
         */
        //String contentType = new MimeTypeUtils().probeContentType(filepath);
        String contentType = new Tika().detect(fileName);

        /*We are ready to serve..*/
        serveResource(length, fileName, lastModified, contentType);
    }

    /**
     * Actually does the heavy lifting given the parameters
     * @param length
     * @param fileName
     * @param lastModified
     * @param contentType
     * @throws Exception
     */
    private void serveResource( Long length, String fileName, long lastModified, String contentType) throws Exception {

        // Validate request headers for caching ---------------------------------------------------

        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && HttpUtils.matches(ifNoneMatch, fileName)) {
            response.setHeader("ETag", fileName); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
            response.setHeader("ETag", fileName); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // Validate request headers for resume ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && !HttpUtils.matches(ifMatch, fileName)) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412.
        long ifUnmodifiedSince = request.getDateHeader("If-Unmodified-Since");
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // Validate and process range -------------------------------------------------------------

        // Prepare some variables. The full Range represents the complete file.
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = new ArrayList<>();

        // Validate and process Range and If-Range headers.
        String range = request.getHeader("Range");
        if (range != null) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            String ifRange = request.getHeader("If-Range");
            if (ifRange != null && !ifRange.equals(fileName)) {
                try {
                    long ifRangeTime = request.getDateHeader("If-Range"); // Throws IAE if invalid.
                    if (ifRangeTime != -1) {
                        ranges.add(full);
                    }
                } catch (IllegalArgumentException ignore) {
                    ranges.add(full);
                }
            }

            // If any valid If-Range header, then process each part of byte range.
            if (ranges.isEmpty()) {
                for (String part : range.substring(6).split(",")) {
                    // Assuming a file with length of 100, the following examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                    long start = Range.sublong(part, 0, part.indexOf("-"));
                    long end = Range.sublong(part, part.indexOf("-") + 1, part.length());

                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) {
                        end = length - 1;
                    }

                    // Check if Range is syntactically valid. If not, then return 416.
                    if (start > end) {
                        response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }

                    // Add range.
                    ranges.add(new Range(start, end, length));
                }
            }
        }

        // Prepare and initialize response --------------------------------------------------------

        // Get content type by file name and set content disposition.
        String disposition = "inline";

        // If content type is unknown, then set the default value.
        // For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
        // To add new content types, add new mime-mapping entry in web.xml.
        if (contentType == null) {
            contentType = "application/octet-stream";
        } else if (!contentType.startsWith("image")) {
            // Else, expect for images, determine content disposition. If content type is supported by
            // the browser, then set to inline, else attachment which will pop a 'save as' dialogue.
            String accept = request.getHeader("Accept");
            disposition = accept != null && HttpUtils.accepts(accept, contentType) ? "inline" : "attachment";
        }
        logger.debug("Content-Type : {}", contentType);
        // Initialize response.
        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setHeader("Content-Type", contentType);
        response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileName + "\"");
        logger.debug("Content-Disposition : {}", disposition);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", fileName);
        response.setDateHeader("Last-Modified", lastModified);
        response.setDateHeader("Expires", System.currentTimeMillis() + DEFAULT_EXPIRE_TIME);

        // Send requested file (part(s)) to client ------------------------------------------------

        // Prepare streams.

        /*
            Switcharoo Here Here!
         */
        InputStream source;
        if(useFile){
            source = new BufferedInputStream(Files.newInputStream(filepath));
        } else {
            source = gridFSDBFile.getInputStream();
        }

        //try ();
        try (InputStream input = new BufferedInputStream(source);
             OutputStream output = response.getOutputStream()) {

            if (ranges.isEmpty() || ranges.get(0) == full) {

                // Return full file.
                logger.info("Return full file");
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + full.start + "-" + full.end + "/" + full.total);
                response.setHeader("Content-Length", String.valueOf(full.length));
                Range.copy(input, output, length, full.start, full.length);

            } else if (ranges.size() == 1) {

                // Return single part of file.
                Range r = ranges.get(0);
                logger.info("Return 1 part of file : from ({}) to ({})", r.start, r.end);
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                response.setHeader("Content-Length", String.valueOf(r.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Copy single part range.
                Range.copy(input, output, length, r.start, r.length);

            } else {

                // Return multiple parts of file.
                response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Cast back to ServletOutputStream to get the easy println methods.
                ServletOutputStream sos = (ServletOutputStream) output;

                // Copy multi part range.
                for (Range r : ranges) {
                    logger.info("Return multi part of file : from ({}) to ({})", r.start, r.end);
                    // Add multipart boundary and header fields for every range.
                    sos.println();
                    sos.println("--" + MULTIPART_BOUNDARY);
                    sos.println("Content-Type: " + contentType);
                    sos.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);

                    // Copy single part range of multi part range.
                    Range.copy(input, output, length, r.start, r.length);
                }

                // End with multipart boundary.
                sos.println();
                sos.println("--" + MULTIPART_BOUNDARY + "--");
            }
        }

    }

    private static class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         * @param start Start of the byte range.
         * @param end End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }

        public static long sublong(String value, int beginIndex, int endIndex) {
            String substring = value.substring(beginIndex, endIndex);
            return (substring.length() > 0) ? Long.parseLong(substring) : -1;
        }

        private static void copy(InputStream input, OutputStream output, long inputSize, long start, long length) throws IOException {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int read;

            if (inputSize == length) {
                // Write full range.
                while ((read = input.read(buffer)) > 0) {
                    output.write(buffer, 0, read);
                    output.flush();
                }
            } else {
                input.skip(start);
                long toRead = length;

                while ((read = input.read(buffer)) > 0) {
                    if ((toRead -= read) > 0) {
                        output.write(buffer, 0, read);
                        output.flush();
                    } else {
                        output.write(buffer, 0, (int) toRead + read);
                        output.flush();
                        break;
                    }
                }
            }
        }
    }

    private static class HttpUtils {

        /**
         * Returns true if the given accept header accepts the given value.
         * @param acceptHeader The accept header.
         * @param toAccept The value to be accepted.
         * @return True if the given accept header accepts the given value.
         */
        public static boolean accepts(String acceptHeader, String toAccept) {
            String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
            Arrays.sort(acceptValues);

            return Arrays.binarySearch(acceptValues, toAccept) > -1
                    || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
                    || Arrays.binarySearch(acceptValues, "*/*") > -1;
        }

        /**
         * Returns true if the given match header matches the given value.
         * @param matchHeader The match header.
         * @param toMatch The value to be matched.
         * @return True if the given match header matches the given value.
         */
        public static boolean matches(String matchHeader, String toMatch) {
            String[] matchValues = matchHeader.split("\\s*,\\s*");
            Arrays.sort(matchValues);
            return Arrays.binarySearch(matchValues, toMatch) > -1
                    || Arrays.binarySearch(matchValues, "*") > -1;
        }
    }
}
