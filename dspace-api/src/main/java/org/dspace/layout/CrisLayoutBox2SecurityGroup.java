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
@Table(name = "cris_layout_box2securitygroup")
public class CrisLayoutBox2SecurityGroup implements Serializable {

    @Embeddable
    public static class CrisLayoutBox2SecurityGroupId implements Serializable {
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "box_id")
        private CrisLayoutBox boxId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "group_id")
        private Group groupId;

        public CrisLayoutBox2SecurityGroupId() {

        }

        public CrisLayoutBox2SecurityGroupId(CrisLayoutBox boxId, Group groupId) {
            this.boxId = boxId;
            this.groupId = groupId;
        }

        public CrisLayoutBox getBoxId() {
            return boxId;
        }

        public void setBoxId(CrisLayoutBox boxId) {
            this.boxId = boxId;
        }

        public Group getGroupId() {
            return groupId;
        }

        public void setGroupId(Group groupId) {
            this.groupId = groupId;
        }
    }

    @EmbeddedId
    private CrisLayoutBox2SecurityGroupId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("boxId")
    @JoinColumn(name = "box_id", insertable = false, updatable = false)
    private CrisLayoutBox box;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "alternative_box_id", nullable = true)
    private CrisLayoutBox alternativeBox;

    public CrisLayoutBox2SecurityGroup() {

    }

    public CrisLayoutBox2SecurityGroup(CrisLayoutBox2SecurityGroupId id,
                                       CrisLayoutBox box, Group group,
                                       CrisLayoutBox alternativeBox) {
        this.id = id;
        this.box = box;
        this.group = group;
        this.alternativeBox = alternativeBox;
    }

    public CrisLayoutBox2SecurityGroupId getId() {
        return id;
    }

    public void setId(CrisLayoutBox2SecurityGroupId id) {
        this.id = id;
    }

    public CrisLayoutBox getBox() {
        return box;
    }

    public void setBox(CrisLayoutBox box) {
        this.box = box;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public CrisLayoutBox getAlternativeBox() {
        return alternativeBox;
    }

    public void setAlternativeBox(CrisLayoutBox alternativeBox) {
        this.alternativeBox = alternativeBox;
    }
}
