package net.org.selector.storer.web.rest;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import net.org.selector.storer.domain.FileInfo;
import net.org.selector.storer.domain.ResizeInfo;
import net.org.selector.storer.service.FileService;
import net.org.selector.storer.service.ImageService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tomcat.util.http.fileupload.FileItemHeaders;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.tomcat.util.http.fileupload.servlet.ServletRequestContext;
import org.apache.tomcat.util.http.fileupload.util.LimitedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static com.google.common.base.Objects.equal;

/**
 * SLitvinov on 25.02.2015.
 */
@Controller
@RequestMapping("/files/**")
public class FileResource extends ServletFileUpload implements EnvironmentAware {
    private final Logger log = LoggerFactory.getLogger(FileResource.class);
    private static final int DEFAULT_BUFFER_SIZE = 10240; // ..bytes = 10KB.
    private static final long DEFAULT_EXPIRE_TIME = 604800000L; // ..ms = 1 week.
    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";

    @Inject
    private FileService fileService;
    @Inject
    private ImageService imageService;
    private long sizeMax = -1;

    /**
     * GET  /rest/files/:filename -> get the "filename" file.
     */
    @RequestMapping(method = RequestMethod.GET)
    public void get(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.debug("REST request to get file : {}", request.getRequestURL().toString());
        // URL-decode the file name (might contain spaces and on) and prepare file object.
        String filename = URLDecoder.decode(request.getServletPath(), "UTF-8");
        // Validate the requested file ------------------------------------------------------------

        // Check if file is actually supplied to the request URL.
        if (Strings.isNullOrEmpty(filename)) {
            // Do your thing if the file is not supplied to the request URL.
            // Throw an exception, or send 404, or show default/warning page, or just ignore it.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        GridFSDBFile file = fileService.findOneByName(filename);


        // Check if file actually exists in filesystem.
        if (file == null) {
            // Do your thing if the file appears to be non-existing.
            // Throw an exception, or send 404, or show default/warning page, or just ignore it.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Prepare some variables. The ETag is an unique identifier of the file.
        String fileName = FilenameUtils.getName(file.getFilename());
        long length = file.getLength();
        long lastModified = file.getUploadDate().getTime();
        String eTag = fileName + "_" + length + "_" + lastModified;
        long expires = System.currentTimeMillis() + DEFAULT_EXPIRE_TIME;


        // Validate request headers for caching ---------------------------------------------------

        // If-None-Match header should contain "*" or ETag. If so, then return 304.
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && matches(ifNoneMatch, eTag)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            response.setHeader("ETag", eTag); // Required in 304.
            response.setDateHeader("Expires", expires); // Postpone cache with 1 week.
            return;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
        // This header is ignored if any If-None-Match header is specified.
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            response.setHeader("ETag", eTag); // Required in 304.
            response.setDateHeader("Expires", expires); // Postpone cache with 1 week.
            return;
        }


        // Validate request headers for resume ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412.
        String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && !matches(ifMatch, eTag)) {
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

            // If-Range header should either match ETag or be greater then LastModified. If not,
            // then return full file.
            String ifRange = request.getHeader("If-Range");
            if (ifRange != null && !ifRange.equals(eTag)) {
                try {
                    long ifRangeTime = request.getDateHeader("If-Range"); // Throws IAE if invalid.
                    if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) {
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
                    long start = sublong(part, 0, part.indexOf("-"));
                    long end = sublong(part, part.indexOf("-") + 1, part.length());

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

        // Get content type by file name and set default GZIP support and content disposition.
        String contentType = file.getContentType();
        boolean acceptsGzip = false;
        String disposition = "inline";

        // If content type is unknown, then set the default value.
        // For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
        // To add new content types, add new mime-mapping entry in web.xml.
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // If content type is text, then determine whether GZIP content encoding is supported by
        // the browser and expand content type with the one and right character encoding.
        if (contentType.startsWith("text")) {
            String acceptEncoding = request.getHeader("Accept-Encoding");
            acceptsGzip = acceptEncoding != null && accepts(acceptEncoding, "gzip");
            contentType += ";charset=UTF-8";
        }

        // Else, expect for images, determine content disposition. If content type is supported by
        // the browser, then set to inline, else attachment which will pop a 'save as' dialogue.
        else if (!contentType.startsWith("image")) {
            String accept = request.getHeader("Accept");
            disposition = accept != null && accepts(accept, contentType) ? "inline" : "attachment";
        }

        // Initialize response.
        response.reset();
        response.setBufferSize(DEFAULT_BUFFER_SIZE);
        response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", eTag);
        response.setDateHeader("Last-Modified", lastModified);
        response.setDateHeader("Expires", expires);


        // Send requested file (part(s)) to client ------------------------------------------------

        // Prepare streams.
        InputStream input = null;
        OutputStream output = null;

        try {
            // Open streams.
            input = file.getInputStream();
            output = response.getOutputStream();

            if (ranges.isEmpty() || ranges.get(0) == full) {
                // Return full file.
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + full.start + "-" + full.end + "/" + full.total);

                if (acceptsGzip) {
                    // The browser accepts GZIP, so GZIP the content.
                    response.setHeader("Content-Encoding", "gzip");
                    output = new GZIPOutputStream(output, DEFAULT_BUFFER_SIZE);
                } else {
                    // Content length is not directly predictable in case of GZIP.
                    // So only add it if there is no means of GZIP, else browser will hang.
                    response.setHeader("Content-Length", String.valueOf(full.length));
                }

                // Copy full range.
                IOUtils.copy(input, output);
            } else if (ranges.size() == 1) {

                // Return single part of file.
                Range r = ranges.get(0);
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                response.setHeader("Content-Length", String.valueOf(r.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.

                // Copy single part range.
                IOUtils.copyLarge(input, output, r.start, r.length);
            } else {

                // Return multiple parts of file.
                response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.


                // Cast back to ServletOutputStream to get the easy println methods.
                ServletOutputStream sos = (ServletOutputStream) output;

                // Copy multi part range.
                for (Range r : ranges) {
                    // Add multipart boundary and header fields for every range.
                    sos.println();
                    sos.println("--" + MULTIPART_BOUNDARY);
                    sos.println("Content-Type: " + contentType);
                    sos.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);

                    // Copy single part range of multi part range.
                    IOUtils.copyLarge(input, output, r.start, r.length);
                }

                // End with multipart boundary.
                sos.println();
                sos.println("--" + MULTIPART_BOUNDARY + "--");
            }
        } finally {
            // Gently close streams.
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(input);
        }
    }

    /**
     * POST  /rest/files -> Create a new file.
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<List<FileInfo>> createOrUpdate(HttpServletRequest request) throws IOException, FileUploadException {
        ServletRequestContext ctx = new ServletRequestContext(request);
        String contentType = ctx.getContentType();
        if (!isMultipartContent(ctx)) {
            throw new InvalidContentTypeException(String.format(
                "the request doesn't contain a %s or %s stream, content type header is %s",
                MULTIPART_FORM_DATA, MULTIPART_MIXED, contentType));
        }
        final long requestSize = ctx.contentLength();
        InputStream input;
        if (sizeMax >= 0) {
            if (requestSize != -1 && requestSize > sizeMax) {
                throw new SizeLimitExceededException(String.format(
                    "the request was rejected because its size (%s) exceeds the configured maximum (%s)",
                    requestSize, sizeMax),
                    requestSize, sizeMax);
            }
            input = new LimitedInputStream(ctx.getInputStream(), sizeMax) {
                @Override
                protected void raiseError(long pSizeMax, long pCount)
                    throws IOException {
                    FileUploadException ex = new SizeLimitExceededException(
                        String.format("the request was rejected because its size (%s) exceeds the configured maximum (%s)",
                            pCount, pSizeMax),
                        pCount, pSizeMax);
                    throw new FileUploadIOException(ex);
                }
            };
        } else {
            input = ctx.getInputStream();
        }
        String charEncoding = ctx.getCharacterEncoding();
        byte[] boundary = getBoundary(contentType);
        if (boundary == null) {
            IOUtils.closeQuietly(input); // avoid possible resource leak
            throw new FileUploadException("the request was rejected because no multipart boundary was found");
        }

        MultipartStream multi;
        try {
            multi = new MultipartStream(input, boundary, 4096, null);
        } catch (IllegalArgumentException iae) {
            IOUtils.closeQuietly(input); // avoid possible resource leak
            throw new InvalidContentTypeException(
                String.format("The boundary specified in the %s header is too long", CONTENT_TYPE), iae);
        }
        multi.setHeaderEncoding(charEncoding);
        boolean nextPart = multi.skipPreamble();
        String path = null;
        List<String> imageSizes = Lists.newArrayList();
        while (nextPart) {
            FileItemHeaders headers = getParsedHeaders(multi.readHeaders());
            String fieldName = getFieldName(headers);
            if (equal(fieldName, "path")) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                multi.readBodyData(out);
                path = URLDecoder.decode(new String(out.toByteArray()), "UTF-8");
            } else if (equal(fieldName, "imageSizes")) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                multi.readBodyData(out);
                String sizesStr = URLDecoder.decode(new String(out.toByteArray()), "UTF-8");
                Iterators.addAll(imageSizes, Splitter.on(',').split(sizesStr).iterator());
            } else if (equal(fieldName, "file")) {
                String fileName = computeFileName(request, path, getFileName(headers));
                String subContentType = headers.getHeader(CONTENT_TYPE);
                PipedInputStream in = new PipedInputStream();
                PipedOutputStream out = new PipedOutputStream(in);
                new Thread(
                    () -> {
                        try {
                            multi.readBodyData(out);
                        } catch (IOException e) {
                            log.error("Can't read body data", e);
                        } finally {
                            IOUtils.closeQuietly(out);
                        }
                    }
                ).start();
                try {
                    List<FileInfo> result = Lists.newArrayList();
                    if (imageSizes.size() > 0) {
                        byte[] bytes = IOUtils.toByteArray(in);
                        String mode = null;
                        for (String imageSize : imageSizes) {
                            if (imageSize.contains(":")) {
                                Iterable<String> imageSizeAndMode = Splitter.on(":").split(imageSize);
                                imageSize = Iterables.getFirst(imageSizeAndMode, null);
                                mode = Iterables.get(imageSizeAndMode, 1);
                            }
                            ResizeInfo resizeInfo = imageService.resizeImage(fileName,
                                new ByteArrayInputStream(bytes), subContentType, imageSize, mode);
                            GridFSFile file = fileService.save(resizeInfo.getName(), resizeInfo.getByteArrayInputStream(), resizeInfo.getContentType());
                            FileInfo fileInfo = new FileInfo(FilenameUtils.getName(resizeInfo.getName()), resizeInfo.getName(), String.valueOf(file.getLength()));
                            fileInfo.setHeight(resizeInfo.getHeight());
                            fileInfo.setWidth(resizeInfo.getWidth());
                            result.add(fileInfo);
                        }
                    } else {
                        GridFSFile file = fileService.save(fileName, in, subContentType);
                        result.add(new FileInfo(FilenameUtils.getName(fileName), fileName, String.valueOf(file.getLength())));
                    }

                    return new ResponseEntity<>(result, HttpStatus.OK);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
            nextPart = multi.readBoundary();
        }
        throw new InvalidContentTypeException("the request was rejected because no file filed was found");
    }


    private String computeFileName(HttpServletRequest request, String path, String originalFilename) throws UnsupportedEncodingException {
        String filename;
        if (!Strings.isNullOrEmpty(path)) {
            filename = URLDecoder.decode(path, "UTF-8");
        } else {
            filename = request.getServletPath() + "/" + UUID.randomUUID().toString().replace("-", "") + "/" + originalFilename;
        }
        filename = filename.replace("//", "/");
        return filename;
    }

    private long parseSize(String size) {
        Assert.hasLength(size, "Size must not be empty");
        size = size.toUpperCase();
        if (size.endsWith("KB")) {
            return Long.valueOf(size.substring(0, size.length() - 2)) * 1024;
        }
        if (size.endsWith("MB")) {
            return Long.valueOf(size.substring(0, size.length() - 2)) * 1024 * 1024;
        }
        return Long.valueOf(size);
    }

    /**
     * DELETE  /rest/files/:filename -> delete the "filename" file.
     */
    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Void> delete(HttpServletRequest request) throws UnsupportedEncodingException {
        String filename = request.getServletPath();
        log.debug("REST request to delete file : {}", filename);
        fileService.delete(filename);
        return ResponseEntity.ok().build();
    }

    private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1
            || Arrays.binarySearch(matchValues, "*") > -1;
    }


    /**
     * Returns a substring of the given string value from the given begin index to the given end
     * index as a long. If the substring is empty, then -1 will be returned
     *
     * @param value      The string value to return a substring as long for.
     * @param beginIndex The begin index of the substring to be returned as long.
     * @param endIndex   The end index of the substring to be returned as long.
     * @return A substring of the given string value as long or -1 if substring is empty.
     */
    private static long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }

    @Override
    public void setEnvironment(Environment environment) {
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment);
        sizeMax = parseSize(propertyResolver.getProperty("upload.maxFileSize", String.class, "-1"));
    }

    protected class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         *
         * @param start Start of the byte range.
         * @param end   End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }
    }

    /**
     * Returns true if the given accept header accepts the given value.
     *
     * @param acceptHeader The accept header.
     * @param toAccept     The value to be accepted.
     * @return True if the given accept header accepts the given value.
     */
    private static boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
        Arrays.sort(acceptValues);
        return Arrays.binarySearch(acceptValues, toAccept) > -1
            || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
            || Arrays.binarySearch(acceptValues, "*/*") > -1;
    }

}
