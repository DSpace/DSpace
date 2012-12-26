/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.core.Constants;

@Entity
@Table(name = "bundle")
public class Bundle extends DSpaceObject {
    private int id;
    private String name;
    private Integer primary;
    private List<Item> items;
    private List<Bitstream> bitstreams;

    @Id
    @Column(name = "bundle_id")
    @GeneratedValue
    public int getID() {
        return id;
    }

    @Column(name = "name", nullable = true)
    public String getName() {
        return name;
    }

    @Column(name = "primary_bitstream_id", nullable = true)
    public Integer getPrimary() {
        return primary;
    }

    public void setID(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrimary(Integer primary) {
        this.primary = primary;
    }

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "item2bundle", joinColumns = { @JoinColumn(name = "item_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "bundle_id", nullable = false) })
    public List<Item> getItems() {
        return items;
    }
    
    public void setItems(List<Item> items) {
        this.items = items;
    }

	@Override
	@Transient
	public int getType() {
		return Constants.BUNDLE;
	}

	 @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    @JoinTable(name = "bundle2bitstream", joinColumns = { @JoinColumn(name = "bundle_id") }, inverseJoinColumns = { @JoinColumn(name = "bitstream_id") })
	public List<Bitstream> getBitstreams() {
		return bitstreams;
	}

	public void setBitstreams(List<Bitstream> bitstreams) {
		this.bitstreams = bitstreams;
	}
    
}
