/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.dspace.content.Bitstream;
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

            Bitstream bitstream = findBitstream(context, item, bundleName, bitstreamType);
            if (bitstream == null) {
                return new String[] {};
            }

            InputStream inputStream = bitstreamStorageService.retrieve(context, bitstream);
            if (inputStream == null) {
                return new String[] {};
            }

            String tempFileName = bitstream.getID().toString();
            File tempFile = new File(getTempExportDir(), tempFileName);
            tempFile.createNewFile();
            tempFile.deleteOnExit();
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                IOUtils.copy(inputStream, outputStream);
                return new String[] { tempFile.getName() };
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Bitstream findBitstream(Context context, Item item, String bundleName, String type) throws Exception {
        List<Bundle> bundles = itemService.getBundles(item, bundleName);
        if (CollectionUtils.isEmpty(bundles)) {
            return null;
        }
        return findBitstreamByType(bundles.get(0), type);
    }

    private Bitstream findBitstreamByType(Bundle bundle, String type) {
        return bundle.getBitstreams().stream()
            .filter(bitstream -> hasTypeEqualsTo(bitstream, type))
            .findFirst()
            .orElse(null);
    }

    private boolean hasTypeEqualsTo(Bitstream bitstream, String type) {
        return type.equals(bitstreamService.getMetadataFirstValue(bitstream, "dc", "type", null, Item.ANY));
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

}
