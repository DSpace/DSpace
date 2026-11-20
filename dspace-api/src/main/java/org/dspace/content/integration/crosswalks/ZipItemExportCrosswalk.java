/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkMode;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Implementation of {@link ItemExportCrosswalk} that export all the given items
 * creating a zip.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ZipItemExportCrosswalk implements ItemExportCrosswalk {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipItemExportCrosswalk.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private BitstreamStorageService bitstreamStorageService;

    @Autowired
    private GroupService groupService;

    private String zipName = "items.zip";

    private String entityType;

    private String bitstreamBundle = "ORIGINAL";

    private String metadataFileName;

    private StreamDisseminationCrosswalk crosswalk;

    private CrosswalkMode crosswalkMode = CrosswalkMode.MULTIPLE;

    private List<String> allowedGroups;

    @Override
    public boolean isAuthorized(Context context) {
        if (CollectionUtils.isEmpty(allowedGroups)) {
            return true;
        }

        EPerson ePerson = context.getCurrentUser();
        if (ePerson == null) {
            return allowedGroups.contains(Group.ANONYMOUS);
        }

        return allowedGroups.stream()
            .anyMatch(groupName -> isMemberOfGroupNamed(context, ePerson, groupName));
    }

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return dso.getType() == Constants.ITEM && hasExpectedEntityType((Item) dso);
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {
        this.disseminate(context, Arrays.asList(dso).iterator(), out);
    }

    @Override
    public void disseminate(Context context, Iterator<? extends DSpaceObject> dsoIterator, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        Assert.notNull(metadataFileName, "The name of the metadata file is required to perform a bulk item export");
        Assert.notNull(crosswalk, "An instance of DisseminationCrosswalk is required to perform a bulk item export");
        Assert.notNull(zipName, "The name of the zip to be generated is required to perform a bulk item export");

        if (!isAuthorized(context)) {
            throw new AuthorizeException("The current user is not allowed to perform a zip item export");
        }

        createZip(context, dsoIterator, out);

    }

    private void createZip(Context context, Iterator<? extends DSpaceObject> dsoIterator, OutputStream out)
        throws CrosswalkObjectNotSupported, IOException {

        try (ZipOutputStream zos = new ZipOutputStream(out)) {

            while (dsoIterator.hasNext()) {

                DSpaceObject dso = dsoIterator.next();
                if (!canDisseminate(context, dso)) {
                    throw new CrosswalkObjectNotSupported(
                        "Can only crosswalk an Item with the configured type: " + entityType);
                }

                try {
                    createFolder(context, (Item) dso, zos);
                } catch (Exception ex) {
                    LOGGER.error("An error occurs creating folder for item " + dso.getID(), ex);
                }

            }

        }

    }

    private void createFolder(Context context, Item item, ZipOutputStream zos) throws IOException {

        createMetadataEntry(context, item, zos);

        List<Bitstream> bitstreams = getBitstreamToExport(item);
        for (Bitstream bitstream : bitstreams) {
            try {
                addBitstreamEntry(context, item, bitstream, zos);
            } catch (Exception ex) {
                LOGGER.error("An error occurs adding bitstream " + bitstream.getID()
                    + " to the folder of item " + item.getID(), ex);
            }
        }

    }

    private void createMetadataEntry(Context context, Item item, ZipOutputStream zos) throws IOException {
        ZipEntry metadataEntry = new ZipEntry(getFolderName(item) + "/" + getMetadataFileName());
        zos.putNextEntry(metadataEntry);
        zos.write(getMetadataFileNameContent(context, item));
        zos.closeEntry();
    }

    private byte[] getMetadataFileNameContent(Context context, Item item) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            crosswalk.disseminate(context, item, out);
            return out.toByteArray();
        } catch (CrosswalkException | IOException | SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Bitstream> getBitstreamToExport(Item item) {
        try {
            List<Bitstream> bitstreams = new ArrayList<>();
            for (Bundle bundle : itemService.getBundles(item, bitstreamBundle)) {
                bitstreams.addAll(bundle.getBitstreams());
            }
            return bitstreams;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private void addBitstreamEntry(Context context, Item item, Bitstream bitstream, ZipOutputStream zos)
        throws IOException {

        InputStream bitstreamContent = retrieveContent(context, bitstream);

        ZipEntry bitstreamEntry = new ZipEntry(getFolderName(item) + "/" + getBitstreamFileName(context, bitstream));
        zos.putNextEntry(bitstreamEntry);

        try {
            writeBitstreamContent(bitstreamContent, zos);
        } finally {
            zos.closeEntry();
        }

    }

    private void writeBitstreamContent(InputStream content, ZipOutputStream zos) throws IOException {
        byte[] bytes = new byte[1024];
        int length;
        while ((length = content.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
    }

    private String getBitstreamFileName(Context context, Bitstream bitstream) {
        String name = "bitstream_" + bitstream.getID().toString();
        return getBitstreamExtension(context, bitstream)
            .map(extension -> name + "." + extension)
            .orElse(name);
    }

    private Optional<String> getBitstreamExtension(Context context, Bitstream bitstream) {
        try {
            return bitstream.getFormat(context).getExtensions().stream().findFirst();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream retrieveContent(Context context, Bitstream bitstream) {
        try {
            return bitstreamStorageService.retrieve(context, bitstream);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getMetadataFileName() {
        return metadataFileName;
    }

    private String getFolderName(Item item) {
        return item.getID().toString();
    }

    private boolean isMemberOfGroupNamed(Context context, EPerson ePerson, String groupName) {
        try {
            Group group = groupService.findByName(context, groupName);
            return groupService.isMember(context, ePerson, group);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getMIMEType() {
        return "application/octet-stream";
    }

    public void setCrosswalkMode(CrosswalkMode crosswalkMode) {
        this.crosswalkMode = crosswalkMode;
    }

    @Override
    public CrosswalkMode getCrosswalkMode() {
        return Optional.ofNullable(this.crosswalkMode).orElse(CrosswalkMode.MULTIPLE);
    }

    private boolean hasExpectedEntityType(Item item) {
        if (StringUtils.isBlank(entityType)) {
            return true;
        }
        return entityType.equals(itemService.getEntityType(item));
    }

    @Override
    public String getFileName() {
        return getZipName();
    }

    public String getZipName() {
        return zipName;
    }

    public void setZipName(String zipName) {
        this.zipName = zipName;
    }

    public Optional<String> getEntityType() {
        return Optional.ofNullable(entityType);
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public StreamDisseminationCrosswalk getCrosswalk() {
        return crosswalk;
    }

    public void setCrosswalk(StreamDisseminationCrosswalk crosswalk) {
        this.crosswalk = crosswalk;
    }

    public String getBitstreamBundle() {
        return bitstreamBundle;
    }

    public void setBitstreamBundle(String bitstreamBundle) {
        this.bitstreamBundle = bitstreamBundle;
    }

    public void setMetadataFileName(String metadataFileName) {
        this.metadataFileName = metadataFileName;
    }

    public List<String> getAllowedGroups() {
        return allowedGroups;
    }

    public void setAllowedGroups(List<String> allowedGroups) {
        this.allowedGroups = allowedGroups;
    }

}
