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

        /**
         * Default constructor.
         */
        public DynamicLayoutBox2SecurityGroupId() {

        }

        /**
         * Creates a composite identifier for the given box and security group.
         *
         * @param boxId the layout box
         * @param groupId the security group
         */
        public DynamicLayoutBox2SecurityGroupId(DynamicLayoutBox boxId, Group groupId) {
            this.boxId = boxId;
            this.groupId = groupId;
        }

        /**
         * Returns the box id.
         */
        public DynamicLayoutBox getBoxId() {
            return boxId;
        }

        /**
         * Sets the box id.
         */
        public void setBoxId(DynamicLayoutBox boxId) {
            this.boxId = boxId;
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

    /**
     * Default constructor.
     */
    public DynamicLayoutBox2SecurityGroup() {

    }

    /**
     * Creates a box-to-security-group association.
     *
     * @param id the composite identifier
     * @param box the layout box
     * @param group the security group
     * @param alternativeBox the alternative box shown when access is denied
     */
    public DynamicLayoutBox2SecurityGroup(DynamicLayoutBox2SecurityGroupId id,
                                       DynamicLayoutBox box, Group group,
                                       DynamicLayoutBox alternativeBox) {
        this.id = id;
        this.box = box;
        this.group = group;
        this.alternativeBox = alternativeBox;
    }

    /**
     * Returns the id.
     */
    public DynamicLayoutBox2SecurityGroupId getId() {
        return id;
    }

    /**
     * Sets the id.
     */
    public void setId(DynamicLayoutBox2SecurityGroupId id) {
        this.id = id;
    }

    /**
     * Returns the box.
     */
    public DynamicLayoutBox getBox() {
        return box;
    }

    /**
     * Sets the box.
     */
    public void setBox(DynamicLayoutBox box) {
        this.box = box;
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
     * Returns the alternative box.
     */
    public DynamicLayoutBox getAlternativeBox() {
        return alternativeBox;
    }

    /**
     * Sets the alternative box.
     */
    public void setAlternativeBox(DynamicLayoutBox alternativeBox) {
        this.alternativeBox = alternativeBox;
    }
}
