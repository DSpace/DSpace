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

import org.dspace.core.ReloadableEntity;

@Entity
@Table(name = "relationship")
public class Relationship implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "relationship_id_seq")
    @SequenceGenerator(name = "relationship_id_seq", sequenceName = "relationship_id_seq", allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false, insertable = true, updatable = false)
    protected Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "left_id", nullable = false)
    private Item leftItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    private RelationshipType relationshipType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "right_id", nullable = false)
    private Item rightItem;

    @Column(name = "left_place")
    private int leftPlace;

    @Column(name = "right_place")
    private int rightPlace;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Item getLeftItem() {
        return leftItem;
    }

    public void setLeftItem(Item leftItem) {
        this.leftItem = leftItem;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    public Item getRightItem() {
        return rightItem;
    }

    public void setRightItem(Item rightItem) {
        this.rightItem = rightItem;
    }

    public int getLeftPlace() {
        return leftPlace;
    }

    public void setLeftPlace(int leftPlace) {
        this.leftPlace = leftPlace;
    }

    public int getRightPlace() {
        return rightPlace;
    }

    public void setRightPlace(int rightPlace) {
        this.rightPlace = rightPlace;
    }

    public Integer getID() {
        return id;
    }
}
