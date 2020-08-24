/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import org.dspace.app.rest.RestResourceController;

/**
 * The Layout section REST resource related to the explore functionality.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class CrisLayoutSectionRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY = RestModel.LAYOUT;
    public static final String NAME = "section";

    private List<List<CrisLayoutSectionComponentRest>> componentRows = new LinkedList<>();

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class<?> getController() {
        return RestResourceController.class;
    }

    public List<List<CrisLayoutSectionComponentRest>> getComponentRows() {
        return componentRows;
    }

    public void setComponentRows(List<List<CrisLayoutSectionComponentRest>> componentRows) {
        this.componentRows = componentRows;
    }

    /**
     * Interface to mark CRIS layout section component REST resources.
     *
     * @author Luca Giamminonni (luca.giamminonni at 4science.it)
     */
    public static interface CrisLayoutSectionComponentRest {

        /**
         * Returns the component type.
         *
         * @return the component type as String
         */
        @JsonGetter
        public String getComponentType();

        /**
         * Returns the component style classes.
         *
         * @return the style as String
         */
        public String getStyle();
    }

    public static class CrisLayoutBrowseComponentRest implements CrisLayoutSectionComponentRest {

        private List<String> browseNames;

        private String style;

        @Override
        public String getComponentType() {
            return "browse";
        }

        public List<String> getBrowseNames() {
            return browseNames;
        }

        public void setBrowseNames(List<String> browseNames) {
            this.browseNames = browseNames;
        }

        @Override
        public String getStyle() {
            return this.style;
        }

        /**
         * @param style the style to set
         */
        public void setStyle(String style) {
            this.style = style;
        }

    }

    public static class CrisLayoutTopComponentRest implements CrisLayoutSectionComponentRest {

        private String discoveryConfigurationName;

        private String sortField;

        private String order;

        private String style;

        public String getDiscoveryConfigurationName() {
            return discoveryConfigurationName;
        }

        public void setDiscoveryConfigurationName(String discoveryConfigurationName) {
            this.discoveryConfigurationName = discoveryConfigurationName;
        }

        public String getSortField() {
            return sortField;
        }

        public void setSortField(String sortField) {
            this.sortField = sortField;
        }

        public String getOrder() {
            return order;
        }

        public void setOrder(String order) {
            this.order = order;
        }

        @Override
        public String getComponentType() {
            return "top";
        }

        @Override
        public String getStyle() {
            return this.style;
        }

        /**
         * @param style the style to set
         */
        public void setStyle(String style) {
            this.style = style;
        }

    }

    public static class CrisLayoutFacetComponentRest implements CrisLayoutSectionComponentRest {

        private String discoveryConfigurationName;

        private String style;

        public String getDiscoveryConfigurationName() {
            return discoveryConfigurationName;
        }

        public void setDiscoveryConfigurationName(String discoveryConfigurationName) {
            this.discoveryConfigurationName = discoveryConfigurationName;
        }

        @Override
        public String getComponentType() {
            return "facet";
        }

        @Override
        public String getStyle() {
            return this.style;
        }

        /**
         * @param style the style to set
         */
        public void setStyle(String style) {
            this.style = style;
        }

    }

    public static class CrisLayoutSearchComponentRest implements CrisLayoutSectionComponentRest {

        private String discoveryConfigurationName;

        private String style;

        public String getDiscoveryConfigurationName() {
            return discoveryConfigurationName;
        }

        public void setDiscoveryConfigurationName(String discoveryConfigurationName) {
            this.discoveryConfigurationName = discoveryConfigurationName;
        }

        @Override
        public String getComponentType() {
            return "search";
        }

        @Override
        public String getStyle() {
            return this.style;
        }

        /**
         * @param style the style to set
         */
        public void setStyle(String style) {
            this.style = style;
        }

    }

}
