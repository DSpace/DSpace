/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * this is an extensionof {@link DynamicLayoutTopComponent} that allows display of
 * set discovery query on many columns.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class DynamicLayoutMultiColumnTopComponent implements DynamicLayoutSectionComponent {

    private String discoveryConfigurationName;

    private String sortField;

    private String order;

    private String style;

    private String titleKey;

    private Integer numberOfItems;

    private List<Column> columns = new ArrayList<>();

    /**
     * Returns the discovery configuration name.
     */
    public String getDiscoveryConfigurationName() {
        return discoveryConfigurationName;
    }

    /**
     * Sets the discovery configuration name.
     */
    public void setDiscoveryConfigurationName(String discoveryConfigurationName) {
        this.discoveryConfigurationName = discoveryConfigurationName;
    }

    /**
     * Returns the sort field.
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * Sets the sort field.
     */
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    /**
     * Returns the order.
     */
    public String getOrder() {
        return order;
    }

    /**
     * Sets the order.
     */
    public void setOrder(String order) {
        this.order = order;
    }

    /**
     * Returns the title key.
     */
    public String getTitleKey() {
        return titleKey;
    }

    /**
     * Sets the title key.
     */
    public void setTitleKey(String titleKey) {
        this.titleKey = titleKey;
    }

    /**
     * Sets the columns.
     */
    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    /**
     * Returns the columns.
     */
    public List<Column> getColumns() {
        return columns;
    }

    @Override
    public String getStyle() {
        return style;
    }

    /**
     * Sets the style.
     */
    public void setStyle(String style) {
        this.style = style;
    }


    /**
     * Sets the number of items.
     */
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
        private String style;
        private String metadataField;
        private String titleKey;

        /**
         * Returns the style.
         */
        public String getStyle() {
            return style;
        }

        /**
         * setter for column style (i.e. width)
         * @param style
         */
        public void setStyle(String style) {
            this.style = style;
        }

        /**
         * Returns the metadata field.
         */
        public String getMetadataField() {
            return metadataField;
        }

        /**
         * metadata to which the value shall be displayed in column
         * @param metadataField
         */
        public void setMetadataField(String metadataField) {
            this.metadataField = metadataField;
        }

        /**
         * Returns the title key.
         */
        public String getTitleKey() {
            return titleKey;
        }

        /**
         * key for the title that will be displayed. If not set metadata name will be used as title
         * @param titleKey
         */
        public void setTitleKey(String titleKey) {
            this.titleKey = titleKey;
        }
    }
}
