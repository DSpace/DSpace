/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.filler;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Utils;

/**
 * Class that model an ItemMetadataImportFiller configuration related to a
 * specific metadata.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class MetadataConfiguration {

    private Map<String, MappingDetails> mapping;

    private Boolean updateEnabled;

    public Map<String, MappingDetails> getMapping() {
        return mapping;
    }

    public void setMapping(Map<String, MappingDetails> mapping) {
        this.mapping = mapping;
    }

    public Boolean getUpdateEnabled() {
        return updateEnabled;
    }

    public void setUpdateEnabled(Boolean updateEnabled) {
        this.updateEnabled = updateEnabled;
    }

    public static class MappingDetails {

        private boolean useAll;

        private Integer visibility;

        private boolean appendMode = false;

        private String targetMetadata;
        private String targetMetadataSchema;
        private String targetMetadataElement;
        private String targetMetadataQualifier;

        private String constantValue;

        public boolean isAppendMode() {
            return appendMode;
        }

        public void setAppendMode(boolean appendMode) {
            this.appendMode = appendMode;
        }

        public boolean isUseAll() {
            return useAll;
        }

        public void setUseAll(boolean useAll) {
            this.useAll = useAll;
        }

        public Integer getVisibility() {
            return visibility;
        }

        public void setVisibility(Integer visibility) {
            this.visibility = visibility;
        }

        public String getTargetMetadata() {
            return targetMetadata;
        }

        public void setTargetMetadata(String targetMetadata) {
            this.targetMetadata = targetMetadata;
            String[] tokens = Utils.tokenize(targetMetadata);
            this.targetMetadataSchema = StringUtils.stripToNull(tokens[0]);
            this.targetMetadataElement = StringUtils.stripToNull(tokens[1]);
            this.targetMetadataQualifier = StringUtils.stripToNull(tokens[2]);
        }

        public String getTargetMetadataSchema() {
            return targetMetadataSchema;
        }

        public String getTargetMetadataElement() {
            return targetMetadataElement;
        }

        public String getTargetMetadataQualifier() {
            return targetMetadataQualifier;
        }

        public String getConstantValue() {
            return constantValue;
        }

        public void setConstantValue(String constantValue) {
            this.constantValue = constantValue;
        }
    }

}
