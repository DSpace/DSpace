/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.converter.MetadataBitstreamWrapperConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.MetadataBitstreamWrapperRest;
import org.dspace.app.rest.model.wrapper.MetadataBitstreamWrapper;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizationBitstreamUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.MissingLicenseAgreementException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.PreviewContent;
import org.dspace.content.Thumbnail;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.PreviewContentService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.dspace.util.FileInfo;
import org.dspace.util.FileTreeViewGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/**
 * This controller returns content of the bitstream to the `Preview` box in the Item View.
 */
@Component(MetadataBitstreamWrapperRest.CATEGORY + "." + MetadataBitstreamWrapperRest.NAME)
public class MetadataBitstreamRestRepository extends DSpaceRestRepository<MetadataBitstreamWrapperRest, Integer> {
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataBitstreamRestRepository.class);
    private final String ARCHIVE_TYPE_ZIP = "zip";
    private final String ARCHIVE_TYPE_TAR = "tar";
    // This constant is used to limit the length of the preview content stored in the database to prevent
    // the database from being overloaded with large amounts of data.
    private static final int MAX_PREVIEW_COUNT_LENGTH = 2000;

    @Autowired
    HandleService handleService;

    @Autowired
    BitstreamConverter bitstreamConverter;

    @Autowired
    MetadataBitstreamWrapperConverter metadataBitstreamWrapperConverter;
    @Autowired
    ItemService itemService;
    @Autowired
    ClarinLicenseResourceMappingService licenseService;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    BitstreamStorageService bitstreamStorageService;

    @Autowired
    AuthorizationBitstreamUtils authorizationBitstreamUtils;

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    PreviewContentService previewContentService;

    // Configured ZIP file preview limit (default: 1000) - if the ZIP file contains more files, it will be truncated
    @Value("${file.preview.zip.limit.length:1000}")
    private int maxPreviewCount;

    @SearchRestMethod(name = "byHandle")
    public Page<MetadataBitstreamWrapperRest> findByHandle(@Parameter(value = "handle", required = true) String handle,
                                                           @Parameter(value = "fileGrpType") String fileGrpType,
                                                           Pageable pageable)
            throws SQLException, ParserConfigurationException, IOException, SAXException, AuthorizeException,
            ArchiveException {
        if (StringUtils.isBlank(handle)) {
            throw new DSpaceBadRequestException("handle cannot be null!");
        }
        List<MetadataBitstreamWrapper> metadataValueWrappers = new ArrayList<>();
        Context context = obtainContext();
        if (Objects.isNull(context)) {
            throw new RuntimeException("Cannot obtain the context from the request.");
        }
        HttpServletRequest request = getRequestService().getCurrentRequest().getHttpServletRequest();
        String contextPath = request.getContextPath();
        List<MetadataBitstreamWrapperRest> rs = new ArrayList<>();
        DSpaceObject dso = null;

        try {
            dso = handleService.resolveToObject(context, handle);
        } catch (Exception e) {
            throw new RuntimeException("Cannot resolve handle: " + handle);
        }

        if (!(dso instanceof Item)) {
            throw new UnprocessableEntityException("Cannot fetch bitstreams from different object than Item.");
        }

        Item item = (Item) dso;
        List<String> fileGrpTypes = Arrays.asList(fileGrpType.split(","));
        List<Bundle> bundles = findEnabledBundles(fileGrpTypes, item);
        for (Bundle bundle : bundles) {
            List<Bitstream> bitstreams = bundle.getBitstreams();
            String use = bundle.getName();
            if (StringUtils.equals("THUMBNAIL", use)) {
                Thumbnail thumbnail = itemService.getThumbnail(context, item, false);
                if (Objects.nonNull(thumbnail)) {
                    bitstreams = new ArrayList<>();
                    bitstreams.add(thumbnail.getThumb());
                }
            }

            for (Bitstream bitstream : bitstreams) {
                String url = composePreviewURL(context, item, bitstream, contextPath);
                List<FileInfo> fileInfos = new ArrayList<>();
                boolean canPreview = findOutCanPreview(context, bitstream);
                if (canPreview) {
                    List<PreviewContent> prContents = previewContentService.findRootByBitstream(context,
                            bitstream.getID());
                    // Generate new content if we didn't find any
                    if (prContents.isEmpty()) {
                        fileInfos = getFilePreviewContent(context, bitstream, fileInfos);
                        // Do not store HTML content in the database because it could be longer than the limit
                        // of the database column
                        if (!StringUtils.equals("text/html", bitstream.getFormat(context).getMIMEType())) {
                            for (FileInfo fi : fileInfos) {
                                createPreviewContent(context, bitstream, fi);
                            }
                        }
                    } else {
                        for (PreviewContent pc : prContents) {
                            fileInfos.add(createFileInfo(pc));
                        }
                    }
                }
                MetadataBitstreamWrapper bts = new MetadataBitstreamWrapper(bitstream, fileInfos,
                        bitstream.getFormat(context).getMIMEType(),
                        bitstream.getFormatDescription(context), url, canPreview);
                metadataValueWrappers.add(bts);
                rs.add(metadataBitstreamWrapperConverter.convert(bts, utils.obtainProjection()));
            }
            context.commit();
        }

        return new PageImpl<>(rs, pageable, rs.size());
    }

    /**
     * Check if the user is requested a specific bundle or all bundles.
     */
    protected List<Bundle> findEnabledBundles(List<String> fileGrpTypes, Item item) {
        List<Bundle> bundles;
        if (fileGrpTypes.size() == 0) {
            bundles = item.getBundles();
        } else {
            bundles = new ArrayList<Bundle>();
            for (String fileGrpType : fileGrpTypes) {
                for (Bundle newBundle : item.getBundles(fileGrpType)) {
                    bundles.add(newBundle);
                }
            }
        }

        return bundles;
    }

    /**
     * Return converted ZIP file content into FileInfo classes.
     * @param context DSpace context object
     * @param bitstream ZIP file bitstream
     * @param fileInfos List which will be returned
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws ArchiveException
     * @throws SAXException
     */
    private List<FileInfo> getFilePreviewContent(Context context, Bitstream bitstream, List<FileInfo> fileInfos)
            throws SQLException, AuthorizeException, IOException, ParserConfigurationException,
            ArchiveException, SAXException {
        InputStream inputStream = null;
        try {
            inputStream = bitstreamService.retrieve(context, bitstream);
        } catch (MissingLicenseAgreementException e) { /* Do nothing */ }

        if (Objects.nonNull(inputStream)) {
            try {
                fileInfos = processInputStreamToFilePreview(context, bitstream, fileInfos, inputStream);
            } catch (IllegalStateException e) {
                log.error("Cannot process Input Stream to file preview because: " + e.getMessage());
            }
        }
        return fileInfos;
    }

    /**
     * Define the hierarchy organization for preview content and file info.
     * The hierarchy is established by the sub map.
     * If a content item contains a sub map, it is considered a directory; if not, it is a file.
     * @param sourceMap  sub map that is used as a pattern
     * @param creator    creator function
     * @return           created sub map
     */
    private <T, U> Hashtable<String, T> createSubMap(Map<String, U> sourceMap, Function<U, T> creator) {
        if (sourceMap == null) {
            return null;
        }

        Hashtable<String, T> sub = new Hashtable<>();
        for (Map.Entry<String, U> entry : sourceMap.entrySet()) {
            sub.put(entry.getKey(), creator.apply(entry.getValue()));
        }
        return sub;
    }

    /**
     * Create file info from preview content.
     * @param pc  preview content
     * @return    created file info
     */
    private FileInfo createFileInfo(PreviewContent pc) {
        Hashtable<String, FileInfo> sub = createSubMap(pc.sub, this::createFileInfo);
        return new FileInfo(pc.name, pc.content, pc.size, pc.isDirectory, sub);
    }

    /**
     * Create preview content from file info for bitstream.
     * @param context   DSpace context object
     * @param bitstream bitstream
     * @param fi        file info
     * @return          created preview content
     * @throws SQLException If database error is occurred
     */
    private PreviewContent createPreviewContent(Context context, Bitstream bitstream, FileInfo fi) throws SQLException {
        Hashtable<String, PreviewContent> sub = createSubMap(fi.sub, value -> {
            try {
                return createPreviewContent(context, bitstream, value);
            } catch (SQLException e) {
                String msg = "Database error occurred while creating new preview content " +
                        "for bitstream with ID = " + bitstream.getID() + " Error msg: " + e.getMessage();
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        });
        return previewContentService.create(context, bitstream, fi.name, fi.content, fi.isDirectory, fi.size, sub);
    }

    /**
     * Convert InputStream of the ZIP file into FileInfo classes.
     *
     * @param context DSpace context object
     * @param bitstream previewing bitstream
     * @param fileInfos List which will be returned
     * @param inputStream content of the zip file
     * @return List of FileInfo classes where is wrapped ZIP file content
     * @throws IOException
     * @throws SQLException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ArchiveException
     */
    private List<FileInfo> processInputStreamToFilePreview(Context context, Bitstream bitstream,
                                                           List<FileInfo> fileInfos, InputStream inputStream)
            throws IOException, SQLException, ParserConfigurationException, SAXException, ArchiveException {
        String bitstreamMimeType = bitstream.getFormat(context).getMIMEType();
        if (bitstreamMimeType.equals("text/plain")) {
            String data = getFileContent(inputStream, true);
            fileInfos.add(new FileInfo(data, false));
        } else if (bitstreamMimeType.equals("text/html")) {
            String data = getFileContent(inputStream, false);
            fileInfos.add(new FileInfo(data, false));
        } else {
            String data = "";
            if (bitstream.getFormat(context).getMIMEType().equals("application/zip")) {
                data = extractFile(inputStream, ARCHIVE_TYPE_ZIP);
                try {
                    fileInfos = FileTreeViewGenerator.parse(data);
                } catch (Exception e) {
                    log.error("Cannot extract file content because: {}", e.getMessage());
                }
            } else if (bitstream.getFormat(context).getMIMEType().equals("application/x-tar")) {
                data = extractFile(inputStream, ARCHIVE_TYPE_TAR);
                try {
                    fileInfos = FileTreeViewGenerator.parse(data);
                } catch (Exception e) {
                    log.error("Cannot extract file content because: {}", e.getMessage());
                }
            }
        }
        return fileInfos;
    }

    /**
     * Compose download URL for calling `MetadataBitstreamController` to download single file or
     * all files as a single ZIP file.
     */
    private String composePreviewURL(Context context, Item item, Bitstream bitstream, String contextPath) {
        String identifier = null;
        if (Objects.nonNull(item) && Objects.nonNull(item.getHandle())) {
            identifier = "handle/" + item.getHandle();
        } else if (Objects.nonNull(item)) {
            identifier = "item/" + item.getID();
        } else {
            identifier = "id/" + bitstream.getID();
        }
        String url = contextPath + "/api/" + BitstreamRest.CATEGORY + "/" + BitstreamRest.PLURAL_NAME + "/"
                + identifier;
        try {
            if (bitstream.getName() != null) {
                url += "/" + Util.encodeBitstreamName(bitstream.getName(), "UTF-8");
            }
        } catch (UnsupportedEncodingException uee) {
            log.error("UnsupportedEncodingException", uee);
        }
        url += "?sequence=" + bitstream.getSequenceID();

        String isAllowed = "n";
        try {
            if (authorizeService.authorizeActionBoolean(context, bitstream, Constants.READ)) {
                isAllowed = "y";
            }
        } catch (SQLException e) {
            log.error("Cannot authorize bitstream action because: " + e.getMessage());
        }

        url += "&isAllowed=" + isAllowed;
        return url;
    }

    /**
     * Creates a temporary file with the appropriate extension based on the specified file type.
     * @param fileType the type of file for which to create a temporary file
     * @return a Path object representing the temporary file
     * @throws IOException if an I/O error occurs while creating the file
     */
    private Path createTempFile(String fileType) throws IOException {
        String extension = ARCHIVE_TYPE_TAR.equals(fileType) ?
                String.format(".%s", ARCHIVE_TYPE_TAR) : String.format(".%s", ARCHIVE_TYPE_ZIP);
        return Files.createTempFile("temp", extension);
    }

    /**
     * Adds a file path and its size to the list of file paths.
     * If the path represents a directory, appends a "/" to the path.
     * @param filePaths the list of file paths to add to
     * @param path the file or directory path
     * @param size the size of the file or directory
     */
    private void addFilePath(List<String> filePaths, String path, long size) {
        String fileInfo = (Files.isDirectory(Paths.get(path))) ? path + "/|" + size : path + "|" + size;
        filePaths.add(fileInfo);
    }

    /**
     * Processes a TAR file, extracting its entries and adding their paths to the provided list.
     * @param filePaths the list to populate with the extracted file paths
     * @param tempFile  the temporary TAR file to process
     * @throws IOException if an I/O error occurs while reading the TAR file
     */
    private void processTarFile(List<String> filePaths, Path tempFile) throws IOException {
        try (InputStream fi = Files.newInputStream(tempFile);
             TarArchiveInputStream tis = new TarArchiveInputStream(fi)) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                addFilePath(filePaths, entry.getName(), entry.getSize());
            }
        }
    }

    /**
     * Processes a ZIP file, extracting its entries and adding their paths to the provided list.
     * @param filePaths      the list to populate with the extracted file paths
     * @param zipFileSystem  the FileSystem object representing the ZIP file
     * @throws IOException if an I/O error occurs while reading the ZIP file
     */
    private void processZipFile(List<String> filePaths, FileSystem zipFileSystem) throws IOException {
        Path root = zipFileSystem.getPath("/");
        Files.walk(root).forEach(path -> {
            try {
                long fileSize = Files.size(path);
                addFilePath(filePaths, path.toString().substring(1), fileSize);
            } catch (IOException e) {
                log.error("An error occurred while getting the size of the zip file.", e);
            }
        });
    }

    /**
     * Closes the specified FileSystem resource if it is not null.
     * @param zipFileSystem the FileSystem to close
     */
    private void closeFileSystem(FileSystem zipFileSystem) {
        if (Objects.nonNull(zipFileSystem)) {
            try {
                zipFileSystem.close();
            } catch (IOException e) {
                log.error("An error occurred while closing the zip file.", e);
            }
        }
    }

    /**
     * Deletes the specified temporary file if it is not null.
     * @param tempFile the Path object representing the temporary file to delete
     */
    private void deleteTempFile(Path tempFile) {
        if (Objects.nonNull(tempFile)) {
            try {
                Files.delete(tempFile);
            } catch (IOException e) {
                log.error("An error occurred while deleting temp file.", e);
            }
        }
    }

    /**
     * Builds an XML response string based on the provided list of file paths.
     * @param filePaths the list of file paths to include in the XML response
     * @return an XML string representation of the file paths
     */
    private String buildXmlResponse(List<String> filePaths) {
        // Is a folder regex
        String folderRegex = "/|\\d+";
        Pattern pattern = Pattern.compile(folderRegex);

        StringBuilder sb = new StringBuilder();
        sb.append("<root>");
        Iterator<String> iterator = filePaths.iterator();
        int fileCounter = 0;
        while (iterator.hasNext() && fileCounter < maxPreviewCount) {
            String filePath = iterator.next();
            // Check if the file is a folder
            Matcher matcher = pattern.matcher(filePath);
            if (!matcher.matches()) {
                // It is a file
                fileCounter++;
            }
            sb.append("<element>").append(filePath).append("</element>");
        }

        if (fileCounter > maxPreviewCount) {
            sb.append("<element>...too many files...|0</element>");
        }
        sb.append("</root>");
        return sb.toString();
    }

    /**
     * Extracts files from an InputStream, processes them based on the specified file type (tar or zip),
     * and returns an XML representation of the file paths.
     * @param inputStream the InputStream containing the file data
     * @param fileType    the type of file to extract ("tar" or "zip")
     * @return an XML string representing the extracted file paths
     */
    public String extractFile(InputStream inputStream, String fileType) {
        List<String> filePaths = new ArrayList<>();
        Path tempFile = null;
        FileSystem zipFileSystem = null;

        try {
            // Create a temporary file based on the file type
            tempFile = createTempFile(fileType);

            // Copy the input stream to the temporary file
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            // Process the file based on its type
            if (ARCHIVE_TYPE_TAR.equals(fileType)) {
                processTarFile(filePaths, tempFile);
            } else {
                zipFileSystem = FileSystems.newFileSystem(tempFile, (ClassLoader) null);
                processZipFile(filePaths, zipFileSystem);
            }
        } catch (IOException e) {
            log.error(String.format("An error occurred while extracting file of type %s.", fileType), e);
        } finally {
            closeFileSystem(zipFileSystem);
            deleteTempFile(tempFile);
        }

        return buildXmlResponse(filePaths);
    }

    /**
     * Read input stream and return content as String
     * @param inputStream to read
     * @return content of the inputStream as a String
     * @throws IOException
     */
    public String getFileContent(InputStream inputStream, boolean cutResult) throws IOException {
        StringBuilder content = new StringBuilder();
        // Generate the preview content in the UTF-8 encoding
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException during creating the preview content because: ", e);
        } catch (IOException e) {
            log.error("IOException during creating the preview content because: ", e);
        }

        reader.close();
        return cutResult ? ensureMaxLength(content.toString()) : content.toString();
    }

    /**
     * Trims the input string to ensure it does not exceed the maximum length for the database column.
     * @param input The original string to be trimmed.
     * @return A string that is truncated to the maximum length if necessary.
     */
    private static String ensureMaxLength(String input) {
        if (input == null) {
            return null;
        }

        // Check if the input string exceeds the maximum preview length
        if (input.length() > MAX_PREVIEW_COUNT_LENGTH) {
            // Truncate the string and append " . . ."
            int previewLength = MAX_PREVIEW_COUNT_LENGTH - 6; // Subtract length of " . . ."
            return input.substring(0, previewLength) + " . . .";
        } else {
            // Return the input string as is if it's within the preview length
            return input;
        }
    }

    /**
     * Find out if the bitstream could be previewed/
     * @param context DSpace context object
     * @param bitstream check if this bitstream could be previewed
     * @return true/false
     * @throws SQLException
     * @throws AuthorizeException
     */
    private boolean findOutCanPreview(Context context, Bitstream bitstream) throws SQLException, AuthorizeException {
        try {
            // Check it is allowed by configuration
            boolean isAllowedByCfg = configurationService.getBooleanProperty("file.preview.enabled", true);
            if (!isAllowedByCfg) {
                return false;
            }

            // Check it is allowed by license
            authorizeService.authorizeAction(context, bitstream, Constants.READ);
            return true;
        } catch (MissingLicenseAgreementException e) {
            return false;
        }
    }

    @Override
    public MetadataBitstreamWrapperRest findOne(Context context, Integer integer) {
        return null;
    }

    @Override
    public Page<MetadataBitstreamWrapperRest> findAll(Context context, Pageable pageable) {
        return null;
    }

    @Override
    public Class<MetadataBitstreamWrapperRest> getDomainClass() {
        return MetadataBitstreamWrapperRest.class;
    }
}
