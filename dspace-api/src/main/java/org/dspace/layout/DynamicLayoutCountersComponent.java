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
 *
 * Implementation of {@link DynamicLayoutSectionComponent} that allows definition
 * of a section containing a list of counters.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class DynamicLayoutCountersComponent implements DynamicLayoutSectionComponent {

    private String style = "";
    private List<CounterSettings> counterSettingsList = new ArrayList<>();

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
     * Returns the counter settings list.
     */
    public List<CounterSettings> getCounterSettingsList() {
        return counterSettingsList;
    }

    /**
     * Sets the counter settings list.
     */
    public void setCounterSettingsList(
        List<CounterSettings> counterSettingsList) {
        this.counterSettingsList = counterSettingsList;
    }

    /**
     * This inner class contains attributes for each entity resume as: discovery query to be run to get the count,
     * key representing entity name and icon to be associated to the entity
     */
    public static class CounterSettings {

        private String discoveryConfigurationName;
        private String icon;
        private String label;
        private String link;

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
         * Returns the icon.
         */
        public String getIcon() {
            return icon;
        }

        /**
         * Sets the icon.
         */
        public void setIcon(String icon) {
            this.icon = icon;
        }

        /**
         * Returns the label.
         */
        public String getLabel() {
            return label;
        }

        /**
         * Sets the label.
         */
        public void setLabel(String label) {
            this.label = label;
        }

        /**
         * Returns the link.
         */
        public String getLink() {
            return link;
        }

        /**
         * Sets the link.
         */
        public void setLink(String link) {
            this.link = link;
        }
    }
}
