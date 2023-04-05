/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.rest.model.AccessConditionDTO;
import org.dspace.app.rest.model.BitstreamFormatRest;
import org.dspace.app.rest.model.CheckSumRest;
import org.dspace.app.rest.model.MetadataValueRest;

/**
 * This Java Bean is used to represent a single bitstream with all its metadata
 * and access conditions ({@link AccessConditionDTO}) inside an
 * upload submission section ({@link DataUpload}
 *
 */
public class UploadBitstreamRest extends UploadStatusResponse {

    private UUID uuid;
    private Map<String, List<MetadataValueRest>> metadata = new HashMap<>();
    private List<AccessConditionDTO> accessConditions;
    private BitstreamFormatRest format;
    private Long sizeBytes;
    private CheckSumRest checkSum;
    private String url;

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public CheckSumRest getCheckSum() {
        return checkSum;
    }

    public void setCheckSum(CheckSumRest checkSum) {
        this.checkSum = checkSum;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Map<String, List<MetadataValueRest>> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, List<MetadataValueRest>> metadata) {
        this.metadata = metadata;
    }

    public List<AccessConditionDTO> getAccessConditions() {
        if (accessConditions == null) {
            accessConditions = new ArrayList<>();
        }
        return accessConditions;
    }

    public void setAccessConditions(List<AccessConditionDTO> accessConditions) {
        this.accessConditions = accessConditions;
    }

    public BitstreamFormatRest getFormat() {
        return format;
    }

    public void setFormat(BitstreamFormatRest format) {
        this.format = format;
    }
}
