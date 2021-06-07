/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static org.apache.commons.lang.StringUtils.startsWith;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.imageio.ImageIO;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link VirtualField} to retrieve a bitstream from an item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VirtualFieldBitstream implements VirtualField {

    private final static Logger LOGGER = LoggerFactory.getLogger(VirtualFieldBitstream.class);

    private final static String PREVIEW_BUNDLE = "PREVIEW";

    private final static String IMAGE_MIME_TYPE_PREFIX = "image";

    private final static String JPEG_MIME_TYPE = IMAGE_MIME_TYPE_PREFIX + "/jpeg";

    private final static String JPEG_FORMAT = "jpg";

    private final ItemService itemService;

    private final BitstreamStorageService bitstreamStorageService;

    private final BitstreamService bitstreamService;

    private final ConfigurationService configurationService;

    private final Map<String, String> bitstreamTypeMap;

    public VirtualFieldBitstream(ItemService itemService, BitstreamStorageService bitstreamStorageService,
        BitstreamService bitstreamService, ConfigurationService configurationService,
        Map<String, String> bitstreamTypeMap) {
        this.itemService = itemService;
        this.bitstreamStorageService = bitstreamStorageService;
        this.bitstreamService = bitstreamService;
        this.configurationService = configurationService;
        this.bitstreamTypeMap = bitstreamTypeMap;
    }

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {
        String[] virtualFieldName = fieldName.split("\\.");
        if (virtualFieldName.length != 4) {
            LOGGER.warn("Invalid bitstream virtual field: " + fieldName);
            return new String[] {};
        }

        try {

            String bundleName = virtualFieldName[2].toUpperCase();
            String bitstreamType = virtualFieldName[3];
            if (bitstreamTypeMap.containsKey(bitstreamType)) {
                bitstreamType = bitstreamTypeMap.get(bitstreamType);
            }

            Bitstream bitstream = findBitstream(item, bundleName, bitstreamType);
            if (bitstream == null) {
                return new String[] {};
            }

            return writeTemporaryFile(context, bitstream);

        } catch (Exception e) {
            LOGGER.error("Error retrieving bitstream of item {} from virtual field {}", item.getID(), fieldName, e);
            return new String[] {};
        }
    }

    private Bitstream findBitstream(Item item, String bundleName, String type) throws Exception {
        List<Bundle> bundles = itemService.getBundles(item, bundleName);
        if (CollectionUtils.isEmpty(bundles)) {
            return null;
        }

        Bitstream bitstream = findBitstream(bundles, bs -> hasTypeEqualsTo(bs, type)).orElse(null);
        if (bitstream == null || shouldNotUseImagePreview()) {
            return bitstream;
        }

        return findPreview(item, bitstream).orElse(bitstream);
    }

    private boolean hasTypeEqualsTo(Bitstream bitstream, String type) {
        return type.equals(bitstreamService.getMetadataFirstValue(bitstream, "dc", "type", null, Item.ANY));
    }

    private Optional<Bitstream> findPreview(Item item, Bitstream bitstream) throws SQLException {
        List<Bundle> bundles = itemService.getBundles(item, PREVIEW_BUNDLE);
        if (CollectionUtils.isEmpty(bundles)) {
            return Optional.empty();
        }

        return findBitstream(bundles, bs -> startsWith(bs.getName(), bitstream.getName()));
    }

    private Optional<Bitstream> findBitstream(List<Bundle> bundles, Predicate<Bitstream> predicate) {
        return bundles.stream()
            .flatMap(bundle -> bundle.getBitstreams().stream())
            .filter(predicate)
            .findFirst();
    }

    private String[] writeTemporaryFile(Context context, Bitstream bitstream) throws Exception {

        InputStream inputStream = bitstreamStorageService.retrieve(context, bitstream);
        if (inputStream == null) {
            return new String[] {};
        }

        String format = getBitstreamMimeType(context, bitstream);

        if (format != null && format.startsWith(IMAGE_MIME_TYPE_PREFIX) && !format.equals(JPEG_MIME_TYPE)) {
            inputStream = convertToJpeg(inputStream);
        }

        File tempFile = createEmptyTemporaryFile(bitstream);

        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            IOUtils.copy(inputStream, outputStream);
            return new String[] { tempFile.getName() };
        }

    }

    private String getBitstreamMimeType(Context context, Bitstream bitstream) throws SQLException, IOException {

        BitstreamFormat format = bitstream.getFormat(context);
        if (format != null && format.getSupportLevel() != BitstreamFormat.UNKNOWN) {
            return format.getMIMEType();
        } else {
            return new Tika().detect(bitstreamStorageService.retrieve(context, bitstream));
        }

    }

    private File createEmptyTemporaryFile(Bitstream bitstream) throws IOException {
        String tempFileName = bitstream.getID().toString();
        File tempFile = new File(getTempExportDir(), tempFileName);
        tempFile.createNewFile();
        tempFile.deleteOnExit();
        return tempFile;
    }

    private InputStream convertToJpeg(InputStream inputStream) throws IOException {

        BufferedImage image = ImageIO.read(inputStream);

        BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), TYPE_INT_RGB);
        convertedImage.createGraphics().drawImage(image, 0, 0, Color.WHITE, null);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(convertedImage, JPEG_FORMAT, outputStream);

        return new ByteArrayInputStream(outputStream.toByteArray());

    }

    private File getTempExportDir() {
        String tempDir = configurationService.getProperty("crosswalk.virtualfield.bitstream.tempdir", " export");
        File tempExportDir = new File(System.getProperty("java.io.tmpdir"), tempDir);
        if (!tempExportDir.exists()) {
            tempExportDir.mkdirs();
            tempExportDir.deleteOnExit();
        }
        return tempExportDir;
    }

    private boolean shouldNotUseImagePreview() {
        return !configurationService.getBooleanProperty("crosswalk.virtualfield.bitstream.use-preview", true);
    }

}
