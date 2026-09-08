/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.bulkimport.model;

import static java.util.Collections.unmodifiableList;
import static org.apache.commons.collections4.multimap.UnmodifiableMultiValuedMap.unmodifiableMultiValuedMap;

import java.util.List;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.vo.MetadataValueVO;

/*
 * @author Jurgen Mamani
 */
public class UploadDetails implements ChildRow {

    private static final String ORIGINAL_BUNDLE = "ORIGINAL";

    private final String parentId;

    private final int row;

    private final String filePath;

    private final String bundleName;

    private final MultiValuedMap<String, MetadataValueVO> metadata;

    private final Integer bitstreamPosition;

    private final List<AccessCondition> accessConditions;

    private final boolean additionalAccessCondition;

    public UploadDetails(String parentId, int row, String filePath, String bundleName, Integer bitstreamPosition,
        List<AccessCondition> accessConditions, boolean additionalAccessCondition,
        MultiValuedMap<String, MetadataValueVO> metadata) {

        this.parentId = parentId;
        this.row = row + 1;
        this.filePath = filePath;
        this.bundleName = bundleName;
        this.metadata = metadata;
        this.bitstreamPosition = bitstreamPosition;
        this.accessConditions = accessConditions;
        this.additionalAccessCondition = additionalAccessCondition;

    }

    public String getParentId() {
        return parentId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getBundleName() {
        return StringUtils.isNotBlank(bundleName) ? bundleName : ORIGINAL_BUNDLE;
    }

    public MultiValuedMap<String, MetadataValueVO> getMetadata() {
        return unmodifiableMultiValuedMap(metadata);
    }

    public Integer getBitstreamPosition() {
        return bitstreamPosition;
    }

    public List<AccessCondition> getAccessConditions() {
        return unmodifiableList(accessConditions);
    }

    public boolean isAdditionalAccessCondition() {
        return additionalAccessCondition;
    }

    public boolean isNotAdditionalAccessCondition() {
        return !additionalAccessCondition;
    }

    public int getRow() {
        return row;
    }

}
