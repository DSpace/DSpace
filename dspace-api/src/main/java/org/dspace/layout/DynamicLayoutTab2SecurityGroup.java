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

        public DynamicLayoutTab2SecurityGroupId() {

        }

        public DynamicLayoutTab2SecurityGroupId(DynamicLayoutTab tabId, Group groupId) {
            this.tabId = tabId;
            this.groupId = groupId;
        }

        public DynamicLayoutTab getTabId() {
            return tabId;
        }

        public void setTabId(DynamicLayoutTab tabId) {
            this.tabId = tabId;
        }

        public Group getGroupId() {
            return groupId;
        }

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

    public DynamicLayoutTab2SecurityGroup() {

    }

    public DynamicLayoutTab2SecurityGroup(DynamicLayoutTab2SecurityGroupId id,
                                       DynamicLayoutTab tab, Group group,
                                       DynamicLayoutTab alternativeTab) {
        this.id = id;
        this.tab = tab;
        this.group = group;
        this.alternativeTab = alternativeTab;
    }

    public DynamicLayoutTab2SecurityGroupId getId() {
        return id;
    }

    public void setId(DynamicLayoutTab2SecurityGroupId id) {
        this.id = id;
    }

    public DynamicLayoutTab getTab() {
        return tab;
    }

    public void setTab(DynamicLayoutTab tab) {
        this.tab = tab;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public DynamicLayoutTab getAlternativeTab() {
        return alternativeTab;
    }

    public void setAlternativeTab(DynamicLayoutTab alternativeTab) {
        this.alternativeTab = alternativeTab;
    }
}
