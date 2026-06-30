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
@Table(name = "cris_layout_tab2securitygroup")
public class CrisLayoutTab2SecurityGroup implements Serializable {

    @Embeddable
    public static class CrisLayoutTab2SecurityGroupId implements Serializable {
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "tab_id")
        private CrisLayoutTab tabId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "group_id")
        private Group groupId;

        public CrisLayoutTab2SecurityGroupId() {

        }

        public CrisLayoutTab2SecurityGroupId(CrisLayoutTab tabId, Group groupId) {
            this.tabId = tabId;
            this.groupId = groupId;
        }

        public CrisLayoutTab getTabId() {
            return tabId;
        }

        public void setTabId(CrisLayoutTab tabId) {
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
    private CrisLayoutTab2SecurityGroupId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tabId")
    @JoinColumn(name = "tab_id", insertable = false, updatable = false)
    private CrisLayoutTab tab;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "alternative_tab_id")
    private CrisLayoutTab alternativeTab;

    public CrisLayoutTab2SecurityGroup() {

    }

    public CrisLayoutTab2SecurityGroup(CrisLayoutTab2SecurityGroupId id,
                                       CrisLayoutTab tab, Group group,
                                       CrisLayoutTab alternativeTab) {
        this.id = id;
        this.tab = tab;
        this.group = group;
        this.alternativeTab = alternativeTab;
    }

    public CrisLayoutTab2SecurityGroupId getId() {
        return id;
    }

    public void setId(CrisLayoutTab2SecurityGroupId id) {
        this.id = id;
    }

    public CrisLayoutTab getTab() {
        return tab;
    }

    public void setTab(CrisLayoutTab tab) {
        this.tab = tab;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public CrisLayoutTab getAlternativeTab() {
        return alternativeTab;
    }

    public void setAlternativeTab(CrisLayoutTab alternativeTab) {
        this.alternativeTab = alternativeTab;
    }
}
