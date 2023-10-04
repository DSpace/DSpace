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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
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
import org.dspace.content.Thumbnail;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.dspace.util.FileInfo;
import org.dspace.util.FileTreeViewGenerator;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final static int MAX_FILE_PREVIEW_COUNT = 1000;

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
                    fileInfos = getFilePreviewContent(context, bitstream, fileInfos);
                }
                MetadataBitstreamWrapper bts = new MetadataBitstreamWrapper(bitstream, fileInfos,
                        bitstream.getFormat(context).getMIMEType(),
                        bitstream.getFormatDescription(context), url, canPreview);
                metadataValueWrappers.add(bts);
                rs.add(metadataBitstreamWrapperConverter.convert(bts, utils.obtainProjection()));
            }
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
        if (bitstream.getFormat(context).getMIMEType().equals("text/plain")) {
            String data = getFileContent(inputStream);
            fileInfos.add(new FileInfo(data, false));
        } else {
            String data = "";
            if (bitstream.getFormat(context).getExtensions().contains("zip")) {
                data = extractFile(inputStream, "zip");
                fileInfos = FileTreeViewGenerator.parse(data);
            } else if (bitstream.getFormat(context).getExtensions().contains("tar")) {
                ArchiveInputStream is = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.TAR,
                        inputStream);
                data = extractFile(is, "tar");
                fileInfos = FileTreeViewGenerator.parse(data);
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
     * Convert ZIP file into structured String.
     * @param inputStream Input stream with ZIP content
     * @param fileType ZIP/TAR
     * @return structured String
     */
    public String extractFile(InputStream inputStream, String fileType) {
        List<String> filePaths = new ArrayList<>();
        Path tempFile = null;
        FileSystem zipFileSystem = null;

        try {
            switch (fileType) {
                case "tar":
                    tempFile = Files.createTempFile("temp", ".tar");
                    break;
                default:
                    tempFile = Files.createTempFile("temp", ".zip");

            }

            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            zipFileSystem = FileSystems.newFileSystem(tempFile, (ClassLoader) null);
            Path root = zipFileSystem.getPath("/");
            Files.walk(root)
                    .forEach(path -> {
                        try {
                            long fileSize = Files.size(path);
                            if (Files.isDirectory(path)) {
                                filePaths.add(path.toString().substring(1) + "/|" + fileSize );
                            } else {
                                filePaths.add(path.toString().substring(1) + "|" + fileSize );
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zipFileSystem != null) {
                try {
                    zipFileSystem.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (tempFile != null) {
                try {
                    Files.delete(tempFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(("<root>"));
        List<String> allFiles = filePaths;
        int fileCounter = 0;
        for (String filePath : allFiles) {
            if (!filePath.isEmpty() && filePath.length() > 3) {
                if (filePath.contains(".")) {
                    fileCounter++;
                }
                sb.append("<element>");
                sb.append(filePath);
                sb.append("</element>");

                if (fileCounter > MAX_FILE_PREVIEW_COUNT) {
                    sb.append("<element>");
                    sb.append("/|0");
                    sb.append("</element>");
                    sb.append("<element>");
                    sb.append("...too many files...|0");
                    sb.append("</element>");
                    break;
                }
            }
        }
        sb.append(("</root>"));
        return sb.toString();
    }

    /**
     * Read input stream and return content as String
     * @param inputStream to read
     * @return content of the inputStream as a String
     * @throws IOException
     */
    public String getFileContent(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }

        reader.close();
        return content.toString();
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
