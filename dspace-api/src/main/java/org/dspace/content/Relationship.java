/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

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

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

/**
 * This class represents a relationship
 * It has a leftItem and a rightItem which are both DSpaceObjects
 * that have a specified RelationshipType that links them together
 * It also has a left and right place column that works just like a normal DSpace metadata place column
 */
@Entity
@Table(name = "relationship")
public class Relationship implements ReloadableEntity<Integer> {

    /**
     * The Integer ID field for this object
     * This is automatically generated
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "relationship_id_seq")
    @SequenceGenerator(name = "relationship_id_seq", sequenceName = "relationship_id_seq", allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false, insertable = true, updatable = false)
    protected Integer id;

    /**
     * The leftItem property for the Relationship object.
     * This leftItem is a DSpaceObject and is stored as an ID
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "left_id", nullable = false)
    private Item leftItem;

    /**
     * The relationshipType property for this Relationship object
     * This is stored as an ID in the database
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    private RelationshipType relationshipType;

    /**
     * The rightItem property for the Relationship object.
     * This rightItem is a DSpaceObject and is stored as an ID
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "right_id", nullable = false)
    private Item rightItem;

    /**
     * An Integer to describe the left place for this relationship
     */
    @Column(name = "left_place")
    private int leftPlace;

    /**
     * An Integer to describe the right place for this relationship
     */
    @Column(name = "right_place")
    private int rightPlace;

    /**
     * A String containing an alternative value (name variant) for the left side
     */
    @Column(name = "leftward_value")
    private String leftwardValue;

    /**
     * A String containing an alternative value (name variant) for the right side
     */
    @Column(name = "rightward_value")
    private String rightwardValue;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.RelationshipService#create(Context)} }
     */
    protected Relationship() {}
    /**
     * Standard setter for the ID field
     * @param id    The ID to be set
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Standard getter for the leftItem field
     * @return  The leftItem Item object in this relationship
     */
    public Item getLeftItem() {
        return leftItem;
    }

    /**
     * Standard setter for the leftItem field
     * @param leftItem  The leftItem Item object that the leftItem field should be set to
     */
    public void setLeftItem(Item leftItem) {
        this.leftItem = leftItem;
    }

    /**
     * Standard getter for the relationshipType field
     * @return  The relationshipType RelationshipType object in this relationship
     */
    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    /**
     * Standard setter for the relationshipType field for the Relationship
     * @param relationshipType  The relationshipType that will be set in this Relationship
     */
    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    /**
     * Standard getter for the rightItem Item object in this Relationship
     * @return  the rightItem Item object
     */
    public Item getRightItem() {
        return rightItem;
    }

    /**
     * Standard setter for the rightItem Item object in this Relationship
     * @param rightItem The rightItem Item object that will be used in this relationship
     */
    public void setRightItem(Item rightItem) {
        this.rightItem = rightItem;
    }

    /**
     * Standard getter for the leftPlace Integer in this Relationship
     * @return  The leftPlace integer for this relationship
     */
    public int getLeftPlace() {
        return leftPlace;
    }

    /**
     * Standard setter for the leftPlace Integer in this Relationship
     * @param leftPlace the leftPlace Integer that will be used in this relationship
     */
    public void setLeftPlace(int leftPlace) {
        this.leftPlace = leftPlace;
    }

    /**
     * Standard getter for the rightPlace Integer in this Relationship
     * @return  the rightPlace integer for this relationship
     */
    public int getRightPlace() {
        return rightPlace;
    }

    /**
     * Standard setter for the rightPlace Integer in this Relationship
     * @param rightPlace    the rightPlace Integer that will be used in this relationship
     */
    public void setRightPlace(int rightPlace) {
        this.rightPlace = rightPlace;
    }

    /**
     * Standard getter for the leftwardValue String in this Relationship
     * @return  the leftwardValue String for this relationship
     */
    public String getLeftwardValue() {
        return leftwardValue;
    }

    /**
     * Standard setter for the leftwardValue String in this Relationship
     * @param leftwardValue    the leftwardValue String that will be used in this relationship
     */
    public void setLeftwardValue(String leftwardValue) {
        this.leftwardValue = leftwardValue;
    }

    /**
     * Standard getter for the rightwardValue String in this Relationship
     * @return  the rightwardValue string for this relationship
     */
    public String getRightwardValue() {
        return rightwardValue;
    }

    /**
     * Standard setter for the rightwardValue String in this Relationship
     * @param rightwardValue    the rightwardValue String that will be used in this relationship
     */
    public void setRightwardValue(String rightwardValue) {
        this.rightwardValue = rightwardValue;
    }

    /**
     * Standard getter for the ID for this Relationship
     * @return  The ID of this relationship
     */
    public Integer getID() {
        return id;
    }
}
