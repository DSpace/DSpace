/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.core.ReloadableEntity;

/**
 * Class representing a RelationshipType
 * This class contains an Integer ID that will be the unique value and primary key in the database.
 * This key is automatically generated
 * It also has a leftType and rightType EntityType that describes the relationshipType together with a leftLabel and
 * rightLabel.
 * The cardinality properties describe how many of each relations this relationshipType can support
 */
@Entity
@Table(name = "relationship_type")
public class RelationshipType implements ReloadableEntity<Integer> {

    /**
     * The Integer ID used as a primary key for this database object.
     * This is generated by a sequence
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "relationship_type_id_seq")
    @SequenceGenerator(name = "relationship_type_id_seq", sequenceName = "relationship_type_id_seq", allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false, insertable = true, updatable = false)
    protected Integer id;

    /**
     * The leftType EntityType field for the relationshipType
     * This is stored as an ID and cannot be null
     */
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "left_type", nullable = false)
    private EntityType leftType;

    /**
     * The rightType EntityType field for the relationshipType
     * This is stored as an ID and cannot be null
     */
    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "right_type", nullable = false)
    private EntityType rightType;

    /**
     * The leftLabel String field for the relationshipType
     * This is stored as a String and cannot be null
     * This is a textual representation of the name of the relationship that this RelationshipType is connected to
     */
    @Column(name = "left_label", nullable = false)
    private String leftLabel;

    /**
     * The rightLabel String field for the relationshipType
     * This is stored as a String and cannot be null
     * This is a textual representation of the name of the relationship that this RelationshipType is connected to
     */
    @Column(name = "right_label", nullable = false)
    private String rightLabel;

    /**
     * The minimum amount of relations for the leftItem that need to be present at all times
     * This is stored as an Integer
     */
    @Column(name = "left_min_cardinality")
    private int leftMinCardinality;

    /**
     * The maximum amount of relations for the leftItem that can to be present at all times
     * This is stored as an Integer
     */
    @Column(name = "left_max_cardinality")
    private int leftMaxCardinality;

    /**
     * The minimum amount of relations for the rightItem that need to be present at all times
     */
    @Column(name = "right_min_cardinality")
    private int rightMinCardinality;

    /**
     * Tha maximum amount of relations for the rightItem that can be present at all times
     */
    @Column(name = "right_max_cardinality")
    private int rightMaxCardinality;

    /**
     * Standard getter for the ID of this RelationshipType
     * @param id    The ID that this RelationshipType should receive
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Standard getter for The leftType EntityType for this RelationshipType
     * @return  The leftType EntityType of this RelationshipType
     */
    public EntityType getLeftType() {
        return leftType;
    }

    /**
     * Standard setter for the leftType EntityType for this RelationshipType
     * @param leftType  The leftType EntityType that this RelationshipType should receive
     */
    public void setLeftType(EntityType leftType) {
        this.leftType = leftType;
    }

    /**
     * Standard getter for The rightType EntityType for this RelationshipType
     * @return  The rightType EntityType of this RelationshipType
     */
    public EntityType getRightType() {
        return rightType;
    }

    /**
     * Standard setter for the rightType EntityType for this RelationshipType
     * @param rightType  The rightType EntityType that this RelationshipType should receive
     */
    public void setRightType(EntityType rightType) {
        this.rightType = rightType;
    }

    /**
     * Standard getter for the leftLabel String for this RelationshipType
     * @return  The leftLabel String of this RelationshipType
     */
    public String getLeftLabel() {
        return leftLabel;
    }

    /**
     * Standard setter for the leftLabel String for this RelationshipType
     * @param leftLabel The leftLabel String that this RelationshipType should receive
     */
    public void setLeftLabel(String leftLabel) {
        this.leftLabel = leftLabel;
    }

    /**
     * Standard getter for the rightLabel String for this RelationshipType
     * @return  The rightLabel String of this RelationshipType
     */
    public String getRightLabel() {
        return rightLabel;
    }

    /**
     * Standard setter for the rightLabel String for this RelationshipType
     * @param rightLabel The rightLabel String that this RelationshipType should receive
     */
    public void setRightLabel(String rightLabel) {
        this.rightLabel = rightLabel;
    }

    /**
     * Standard getter for the leftMinCardinality Integer for this RelationshipType
     * @return  the leftMinCardinality Integer of this RelationshipType
     */
    public int getLeftMinCardinality() {
        return leftMinCardinality;
    }

    /**
     * Standard setter for the leftMinCardinality Integer for this RelationshipType
     * @param leftMinCardinality    The leftMinCardinality Integer that this RelationshipType should recieve
     */
    public void setLeftMinCardinality(int leftMinCardinality) {
        this.leftMinCardinality = leftMinCardinality;
    }

    /**
     * Standard getter for the leftMaxCardinality Integer for this RelationshipType
     * @return  the leftMaxCardinality Integer of this RelationshipType
     */
    public int getLeftMaxCardinality() {
        return leftMaxCardinality;
    }

    /**
     * Standard setter for the leftMaxCardinality Integer for this RelationshipType
     * @param leftMaxCardinality    The leftMaxCardinality Integer that this RelationshipType should recieve
     */
    public void setLeftMaxCardinality(int leftMaxCardinality) {
        this.leftMaxCardinality = leftMaxCardinality;
    }

    /**
     * Standard getter for the rightMinCardinality Integer for this RelationshipType
     * @return  the rightMinCardinality Integer of this RelationshipType
     */
    public int getRightMinCardinality() {
        return rightMinCardinality;
    }

    /**
     * Standard setter for the rightMinCardinality Integer for this RelationshipType
     * @param rightMinCardinality    The rightMinCardinality Integer that this RelationshipType should recieve
     */
    public void setRightMinCardinality(int rightMinCardinality) {
        this.rightMinCardinality = rightMinCardinality;
    }

    /**
     * Standard getter for the rightMaxCardinality Integer for this RelationshipType
     * @return  the rightMaxCardinality Integer of this RelationshipType
     */
    public int getRightMaxCardinality() {
        return rightMaxCardinality;
    }

    /**
     * Standard setter for the rightMaxCardinality Integer for this RelationshipType
     * @param rightMaxCardinality    The rightMaxCardinality Integer that this RelationshipType should recieve
     */
    public void setRightMaxCardinality(int rightMaxCardinality) {
        this.rightMaxCardinality = rightMaxCardinality;
    }

    /**
     * Standard getter for the ID of this RelationshipType
     * @return  The ID of this RelationshipType
     */
    public Integer getID() {
        return id;
    }
}
