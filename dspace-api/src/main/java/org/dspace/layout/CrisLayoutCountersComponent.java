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
 * Implementation of {@link CrisLayoutSectionComponent} that allows definition
 * of a section containing a list of counters.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class CrisLayoutCountersComponent implements CrisLayoutSectionComponent {

    private String style = "";
    private List<CounterSettings> counterSettingsList = new ArrayList<>();

    @Override
    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public List<CounterSettings> getCounterSettingsList() {
        return counterSettingsList;
    }

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

        public String getDiscoveryConfigurationName() {
            return discoveryConfigurationName;
        }

        public void setDiscoveryConfigurationName(String discoveryConfigurationName) {
            this.discoveryConfigurationName = discoveryConfigurationName;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }
}
