/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonGetter;
import org.dspace.app.rest.RestResourceController;
import org.dspace.layout.DynamicLayoutCountersComponent;
import org.dspace.layout.DynamicLayoutMultiColumnTopComponent;

/**
 * The Layout section REST resource related to the explore functionality.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class DynamicLayoutSectionRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY = RestModel.LAYOUT;
    public static final String NAME = "section";
    public static final String NAME_PLURAL = "sections";

    private List<List<DynamicLayoutSectionComponentRest>> componentRows = new LinkedList<>();

    private List<DynamicLayoutSectionRest> nestedSections = new ArrayList<>();

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return NAME_PLURAL;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class<?> getController() {
        return RestResourceController.class;
    }

    public List<List<DynamicLayoutSectionComponentRest>> getComponentRows() {
        return componentRows;
    }

    public void setComponentRows(List<List<DynamicLayoutSectionComponentRest>> componentRows) {
        this.componentRows = componentRows;
    }

    public List<DynamicLayoutSectionRest> getNestedSections() {
        return nestedSections;
    }

    public void setNestedSections(List<DynamicLayoutSectionRest> nestedSections) {
        this.nestedSections = nestedSections;
    }

    /**
     * Interface to mark CRIS layout section component REST resources.
     *
     * @author Luca Giamminonni (luca.giamminonni at 4science.it)
     */
    public static interface DynamicLayoutSectionComponentRest {

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

    public static class DynamicLayoutBrowseComponentRest implements DynamicLayoutSectionComponentRest {

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

    public static class DynamicLayoutTopComponentRest implements DynamicLayoutSectionComponentRest {

        private String discoveryConfigurationName;

        private String sortField;

        private String order;

        private String style;

        private String titleKey;

        private Integer numberOfItems;

        private boolean showThumbnails;

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

        public String getTitleKey() {
            return titleKey;
        }

        public void setTitleKey(String titleKey) {
            this.titleKey = titleKey;
        }

        public void setNumberOfItems(Integer numberOfItems) {
            this.numberOfItems = numberOfItems;
        }

        /**
         *
         * @return Number of items to be contained in layout section
         */
        public Integer getNumberOfItems() {
            return numberOfItems;
        }

        public boolean isShowThumbnails() {
            return showThumbnails;
        }

        public void setShowThumbnails(boolean showThumbnails) {
            this.showThumbnails = showThumbnails;
        }
    }

    public static class DynamicLayoutFacetComponentRest implements DynamicLayoutSectionComponentRest {

        private String discoveryConfigurationName;

        private String style;

        private Integer facetsPerRow;

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

        public void setFacetsPerRow(Integer facetsPerRow) {
            this.facetsPerRow = facetsPerRow;
        }

        public Integer getFacetsPerRow() {
            return facetsPerRow;
        }
    }

    public static class DynamicLayoutSearchComponentRest implements DynamicLayoutSectionComponentRest {

        private String discoveryConfigurationName;

        private String style;

        private String searchType;

        private Integer initialStatements;

        private boolean displayTitle;

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

        public String getSearchType() {
            return searchType;
        }

        public void setSearchType(String searchType) {
            this.searchType = searchType;
        }

        public void setInitialStatements(Integer initialStatements) {
            this.initialStatements = initialStatements;
        }

        public Integer getInitialStatements() {
            return initialStatements;
        }

        public void setDisplayTitle(boolean displayTitle) {
            this.displayTitle = displayTitle;
        }

        public boolean getDisplayTitle() {
            return displayTitle;
        }
    }

    public static class DynamicLayoutTextRowComponentRest implements DynamicLayoutSectionComponentRest,
        Comparable<DynamicLayoutTextRowComponentRest> {

        private final Integer order;
        private final String style;
        private final String content;
        private final String contentType;

        public DynamicLayoutTextRowComponentRest(Integer order, String style, String content, String contentType) {
            this.order = order;
            this.style = style;
            this.content = content;
            this.contentType = contentType;
        }

        @Override
        public String getComponentType() {
            return "text-row";
        }

        @Override
        public String getStyle() {
            return style;
        }

        public String getContent() {
            return content;
        }

        public String getContentType() {
            return contentType;
        }

        @Override
        public int compareTo(DynamicLayoutTextRowComponentRest other) {
            return this.order.compareTo(other.order);
        }
    }

    public static class DynamicLayoutTextBoxComponentRest implements DynamicLayoutSectionComponentRest {

        private final List<DynamicLayoutTextRowComponentRest> textRows;
        private final String style;

        public DynamicLayoutTextBoxComponentRest(
            List<DynamicLayoutTextRowComponentRest> textRows, String style) {
            this.textRows = textRows;
            this.style = style;
        }


        @Override
        public String getComponentType() {
            return "text-box";
        }

        @Override
        public String getStyle() {
            return style;
        }

        public List<DynamicLayoutTextRowComponentRest> getTextRows() {
            return textRows;
        }
    }

    public static class DynamicLayoutCountersComponentRest implements DynamicLayoutSectionComponentRest {

        private final String style;
        private final List<CounterSettingsRest> counterSettingsList;

        public static DynamicLayoutCountersComponentRest from (DynamicLayoutCountersComponent source) {
            return new DynamicLayoutCountersComponentRest(source.getStyle(),
                source.getCounterSettingsList().stream()
                .map(CounterSettingsRest::from)
                .collect(Collectors.toList())
                );
        }

        @Override
        public String getComponentType() {
            return "counters";
        }

        public String getStyle() {
            return style;
        }

        public List<CounterSettingsRest> getCounterSettingsList() {
            return counterSettingsList;
        }

        private DynamicLayoutCountersComponentRest(String style,
                                                List<CounterSettingsRest> counterSettingsList) {
            this.style = style;
            this.counterSettingsList = counterSettingsList;
        }

        static class CounterSettingsRest {

            private final String discoveryConfigurationName;
            private final String icon;
            private final String entityName;
            private final String link;

            static CounterSettingsRest from (DynamicLayoutCountersComponent.CounterSettings source) {

                return new CounterSettingsRest(source.getDiscoveryConfigurationName(),
                    source.getIcon(),
                    source.getLabel(),
                    source.getLink());
            }

            private CounterSettingsRest(String discoveryConfigurationName, String icon, String entityName,
                                        String link) {
                this.discoveryConfigurationName = discoveryConfigurationName;
                this.icon = icon;
                this.entityName = entityName;
                this.link = link;
            }

            public String getDiscoveryConfigurationName() {
                return discoveryConfigurationName;
            }

            public String getIcon() {
                return icon;
            }

            public String getEntityName() {
                return entityName;
            }

            public String getLink() {
                return link;
            }
        }

    }

    public static class DynamicLayoutMultiColumnTopComponentRest implements DynamicLayoutSectionComponentRest {

        private String discoveryConfigurationName;

        private String sortField;

        private String order;

        private String style;

        private String titleKey;

        private List<Column> columnList = new ArrayList<>();
        private Integer numberOfItems;

        public void setColumnList(
            List<Column> columnList) {
            this.columnList = columnList;
        }

        public List<Column> getColumnList() {
            return columnList;
        }

        @Override
        public String getComponentType() {
            return "multi-column-top";
        }

        @Override
        public String getStyle() {
            return this.style;
        }

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

        public void setStyle(String style) {
            this.style = style;
        }

        public String getTitleKey() {
            return titleKey;
        }

        public void setTitleKey(String titleKey) {
            this.titleKey = titleKey;
        }

        public void setNumberOfItems(Integer numberOfItems) {
            this.numberOfItems = numberOfItems;
        }

        /**
         *
         * @return Number of items to be contained in layout section
         */
        public Integer getNumberOfItems() {
            return numberOfItems;
        }

        public static class Column {
            private final String style;
            private final String metadataField;
            private final String titleKey;

            public static Column from (DynamicLayoutMultiColumnTopComponent.Column column) {
                return new Column(column.getStyle(), column.getMetadataField(),
                    column.getTitleKey());
            }

            private Column(String style, String metadataField, String titleKey) {
                this.style = style;
                this.metadataField = metadataField;
                this.titleKey = titleKey;
            }

            public String getStyle() {
                return style;
            }

            public String getMetadataField() {
                return metadataField;
            }

            public String getTitleKey() {
                return titleKey;
            }
        }
    }

}
