/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.wrapper;

import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.util.FileInfo;

/**
 * Object which handles data for previewing the bitstream content in the Item View page.
 *
 * @author longtv
 */
public class MetadataBitstreamWrapper {
    private Bitstream bitstream;
    private List<FileInfo> fileInfo;
    private String format;
    private String description;
    private String href;

    private boolean canPreview;
    public MetadataBitstreamWrapper() {
    }

    public MetadataBitstreamWrapper(Bitstream bitstream, List<FileInfo> fileInfo, String format, String description,
                                    String href, boolean canPreview) {
        this.bitstream = bitstream;
        this.fileInfo = fileInfo;
        this.format = format;
        this.description = description;
        this.href = href;
        this.canPreview = canPreview;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCanPreview() {
        return canPreview;
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

    public Bitstream getBitstream() {
        return bitstream;
    }

    public void setBitstream(Bitstream bitstream) {
        this.bitstream = bitstream;
    }

    public List<FileInfo> getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(List<FileInfo> fileInfo) {
        this.fileInfo = fileInfo;
    }
}
