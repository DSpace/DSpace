/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import org.dspace.app.rest.RestResourceController;
import org.dspace.util.FileInfo;

/**
 * The MetadataBitstreamWrapper REST Resource
 *
 * @author longtv
 */
public class MetadataBitstreamWrapperRest extends BaseObjectRest<String> {
    public static final String NAME = "metadatabitstream";
    public static final String CATEGORY = RestAddressableModel.CORE;

    private String name;
    private String description;
    private long fileSize;
    private String checksum;
    private List<FileInfo> fileInfo;
    private String format;
    private String href;
    private boolean canPreview;

    public MetadataBitstreamWrapperRest(String name, String description, long fileSize, String checksum,
                                        List<FileInfo> fileInfo, String format, String href, boolean canPreview) {
        this.name = name;
        this.description = description;
        this.fileSize = fileSize;
        this.checksum = checksum;
        this.fileInfo = fileInfo;
        this.format = format;
        this.href = href;
        this.canPreview = canPreview;
    }

    public void setCanPreview(boolean canPreview) {
        this.canPreview = canPreview;
    }

    public boolean isCanPreview() {
        return canPreview;
    }

    public MetadataBitstreamWrapperRest() {
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public List<FileInfo> getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(List<FileInfo> fileInfo) {
        this.fileInfo = fileInfo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }


    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }
}
