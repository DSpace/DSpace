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


@Entity
@Table(name = "relationship_type")
public class RelationshipType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "relationship_type_id_seq")
    @SequenceGenerator(name = "relationship_type_id_seq", sequenceName = "relationship_type_id_seq", allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false, insertable = true, updatable = false)
    protected Integer id;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "left_type", nullable = false)
    private EntityType leftType;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "right_type", nullable = false)
    private EntityType rightType;

    @Column(name = "left_label", nullable = false)
    private String leftLabel;

    @Column(name = "right_label", nullable = false)
    private String rightLabel;

    @Column(name = "left_min_cardinality")
    private int leftMinCardinality;

    @Column(name = "left_max_cardinality")
    private int leftMaxCardinality;

    @Column(name = "right_min_cardinality")
    private int rightMinCardinality;

    @Column(name = "right_max_cardinality")
    private int rightMaxCardinality;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public EntityType getLeftType() {
        return leftType;
    }

    public void setLeftType(EntityType leftType) {
        this.leftType = leftType;
    }

    public EntityType getRightType() {
        return rightType;
    }

    public void setRightType(EntityType rightType) {
        this.rightType = rightType;
    }

    public String getLeftLabel() {
        return leftLabel;
    }

    public void setLeftLabel(String leftLabel) {
        this.leftLabel = leftLabel;
    }

    public String getRightLabel() {
        return rightLabel;
    }

    public void setRightLabel(String rightLabel) {
        this.rightLabel = rightLabel;
    }

    public int getLeftMinCardinality() {
        return leftMinCardinality;
    }

    public void setLeftMinCardinality(int leftMinCardinality) {
        this.leftMinCardinality = leftMinCardinality;
    }

    public int getLeftMaxCardinality() {
        return leftMaxCardinality;
    }

    public void setLeftMaxCardinality(int leftMaxCardinality) {
        this.leftMaxCardinality = leftMaxCardinality;
    }

    public int getRightMinCardinality() {
        return rightMinCardinality;
    }

    public void setRightMinCardinality(int rightMinCardinality) {
        this.rightMinCardinality = rightMinCardinality;
    }

    public int getRightMaxCardinality() {
        return rightMaxCardinality;
    }

    public void setRightMaxCardinality(int rightMaxCardinality) {
        this.rightMaxCardinality = rightMaxCardinality;
    }
}
