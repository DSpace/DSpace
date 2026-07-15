/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.dspace.core.ReloadableEntity;

@Entity
@Table(name = "dynamic_layout_cell")
public class DynamicLayoutCell implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dynamic_layout_cell_id_seq")
    @SequenceGenerator(name = "dynamic_layout_cell_id_seq", sequenceName = "dynamic_layout_cell_id_seq",
            allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false, insertable = true, updatable = false)
    private Integer id;

    @Column(name = "style")
    private String style;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "row")
    private DynamicLayoutRow row;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "cell", cascade = CascadeType.ALL)
    @OrderColumn(name = "position")
    private List<DynamicLayoutBox> boxes = new ArrayList<>();

    @Override
    public Integer getID() {
        return id;
    }

    /**
     * Returns the style.
     */
    public String getStyle() {
        return style;
    }

    /**
     * Sets the style.
     */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * Returns the boxes.
     */
    public List<DynamicLayoutBox> getBoxes() {
        return boxes;
    }

    /**
     * Adds a box to this cell.
     *
     * @param box the box to add
     */
    public void addBox(DynamicLayoutBox box) {
        getBoxes().add(box);
        box.setCell(this);
    }

    /**
     * Returns the row.
     */
    public DynamicLayoutRow getRow() {
        return row;
    }

    /**
     * Sets the row.
     */
    public void setRow(DynamicLayoutRow row) {
        this.row = row;
    }

    /**
     * Sets the boxex.
     */
    public void setBoxex(List<DynamicLayoutBox> boxes) {
        this.boxes = boxes;
    }
}
