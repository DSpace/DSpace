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
@Table(name = "dynamic_layout_box2securitygroup")
public class DynamicLayoutBox2SecurityGroup implements Serializable {

    @Embeddable
    public static class DynamicLayoutBox2SecurityGroupId implements Serializable {
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "box_id")
        private DynamicLayoutBox boxId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "group_id")
        private Group groupId;

        public DynamicLayoutBox2SecurityGroupId() {

        }

        public DynamicLayoutBox2SecurityGroupId(DynamicLayoutBox boxId, Group groupId) {
            this.boxId = boxId;
            this.groupId = groupId;
        }

        public DynamicLayoutBox getBoxId() {
            return boxId;
        }

        public void setBoxId(DynamicLayoutBox boxId) {
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
    private DynamicLayoutBox2SecurityGroupId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("boxId")
    @JoinColumn(name = "box_id", insertable = false, updatable = false)
    private DynamicLayoutBox box;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "alternative_box_id", nullable = true)
    private DynamicLayoutBox alternativeBox;

    public DynamicLayoutBox2SecurityGroup() {

    }

    public DynamicLayoutBox2SecurityGroup(DynamicLayoutBox2SecurityGroupId id,
                                       DynamicLayoutBox box, Group group,
                                       DynamicLayoutBox alternativeBox) {
        this.id = id;
        this.box = box;
        this.group = group;
        this.alternativeBox = alternativeBox;
    }

    public DynamicLayoutBox2SecurityGroupId getId() {
        return id;
    }

    public void setId(DynamicLayoutBox2SecurityGroupId id) {
        this.id = id;
    }

    public DynamicLayoutBox getBox() {
        return box;
    }

    public void setBox(DynamicLayoutBox box) {
        this.box = box;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public DynamicLayoutBox getAlternativeBox() {
        return alternativeBox;
    }

    public void setAlternativeBox(DynamicLayoutBox alternativeBox) {
        this.alternativeBox = alternativeBox;
    }
}
