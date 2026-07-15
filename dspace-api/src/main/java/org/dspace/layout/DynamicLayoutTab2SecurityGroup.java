/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.io.Serializable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.dspace.eperson.Group;

@Entity
@Table(name = "dynamic_layout_tab2securitygroup")
public class DynamicLayoutTab2SecurityGroup implements Serializable {

    @Embeddable
    public static class DynamicLayoutTab2SecurityGroupId implements Serializable {
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "tab_id")
        private DynamicLayoutTab tabId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "group_id")
        private Group groupId;

        /**
         * Default constructor.
         */
        public DynamicLayoutTab2SecurityGroupId() {

        }

        /**
         * Creates a composite identifier for the given tab and security group.
         *
         * @param tabId the layout tab
         * @param groupId the security group
         */
        public DynamicLayoutTab2SecurityGroupId(DynamicLayoutTab tabId, Group groupId) {
            this.tabId = tabId;
            this.groupId = groupId;
        }

        /**
         * Returns the tab id.
         */
        public DynamicLayoutTab getTabId() {
            return tabId;
        }

        /**
         * Sets the tab id.
         */
        public void setTabId(DynamicLayoutTab tabId) {
            this.tabId = tabId;
        }

        /**
         * Returns the group id.
         */
        public Group getGroupId() {
            return groupId;
        }

        /**
         * Sets the group id.
         */
        public void setGroupId(Group groupId) {
            this.groupId = groupId;
        }
    }

    @EmbeddedId
    private DynamicLayoutTab2SecurityGroupId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tabId")
    @JoinColumn(name = "tab_id", insertable = false, updatable = false)
    private DynamicLayoutTab tab;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "alternative_tab_id")
    private DynamicLayoutTab alternativeTab;

    /**
     * Default constructor.
     */
    public DynamicLayoutTab2SecurityGroup() {

    }

    /**
     * Creates a tab-to-security-group association.
     *
     * @param id the composite identifier
     * @param tab the layout tab
     * @param group the security group
     * @param alternativeTab the alternative tab shown when access is denied
     */
    public DynamicLayoutTab2SecurityGroup(DynamicLayoutTab2SecurityGroupId id,
                                       DynamicLayoutTab tab, Group group,
                                       DynamicLayoutTab alternativeTab) {
        this.id = id;
        this.tab = tab;
        this.group = group;
        this.alternativeTab = alternativeTab;
    }

    /**
     * Returns the id.
     */
    public DynamicLayoutTab2SecurityGroupId getId() {
        return id;
    }

    /**
     * Sets the id.
     */
    public void setId(DynamicLayoutTab2SecurityGroupId id) {
        this.id = id;
    }

    /**
     * Returns the tab.
     */
    public DynamicLayoutTab getTab() {
        return tab;
    }

    /**
     * Sets the tab.
     */
    public void setTab(DynamicLayoutTab tab) {
        this.tab = tab;
    }

    /**
     * Returns the group.
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Sets the group.
     */
    public void setGroup(Group group) {
        this.group = group;
    }

    /**
     * Returns the alternative tab.
     */
    public DynamicLayoutTab getAlternativeTab() {
        return alternativeTab;
    }

    /**
     * Sets the alternative tab.
     */
    public void setAlternativeTab(DynamicLayoutTab alternativeTab) {
        this.alternativeTab = alternativeTab;
    }
}
