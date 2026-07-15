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
@Table(name = "dynamic_layout_row")
public class DynamicLayoutRow implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dynamic_layout_row_id_seq")
    @SequenceGenerator(name = "dynamic_layout_row_id_seq", sequenceName = "dynamic_layout_row_id_seq",
            allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false, insertable = true, updatable = false)
    private Integer id;

    @Column(name = "style")
    private String style;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tab")
    private DynamicLayoutTab tab;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "row", cascade = CascadeType.ALL)
    @OrderColumn(name = "position")
    private List<DynamicLayoutCell> cells = new ArrayList<>();

    @Override
    public Integer getID() {
        return id;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<DynamicLayoutCell> getCells() {
        return cells;
    }

    public void addCell(DynamicLayoutCell cell) {
        getCells().add(cell);
        cell.setRow(this);
    }

    public DynamicLayoutTab getTab() {
        return tab;
    }

    public void setTab(DynamicLayoutTab tab) {
        this.tab = tab;
    }

    public void setCells(List<DynamicLayoutCell> cells) {
        this.cells = cells;
    }
}
