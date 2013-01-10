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
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.services.AuthorizationService;
import org.dspace.services.auth.Action;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

@Entity
@Table(name = "bundle")
@Configurable
@SequenceGenerator(name="bundle_gen", sequenceName="bundle_seq")
public class Bundle extends DSpaceObject {
	@Autowired AuthorizationService authService;
	
    private String name;
    private Bitstream primary;
    private List<Item> items;
    private List<Bitstream> bitstreams;

    @Id
    @Column(name = "bundle_id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="bundle_gen")
    public int getID() {
        return id;
    }

    @Column(name = "name", nullable = true)
    public String getName() {
        return name;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_bitstream_id", nullable = true)
    public Bitstream getPrimary() {
        return primary;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrimary(Bitstream primary) {
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
	public DSpaceObjectType getType() {
		return DSpaceObjectType.BUNDLE;
	}

	 @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    @JoinTable(name = "bundle2bitstream", joinColumns = { @JoinColumn(name = "bundle_id") }, inverseJoinColumns = { @JoinColumn(name = "bitstream_id") })
	public List<Bitstream> getBitstreams() {
		return bitstreams;
	}

	public void setBitstreams(List<Bitstream> bitstreams) {
		this.bitstreams = bitstreams;
	}

	@Transient
	public IDSpaceObject getAdminObject(Action action)
    {
        DSpaceObject adminObject = null;
        List<Item> items = this.getItems();
        Item item = null;
        Collection collection = null;
        Community community = null;
        if (items != null && !items.isEmpty())
        {
            item = items.get(0);
            collection = item.getOwningCollection();
            if (collection != null)
            {
                List<Community> communities = collection.getParents();
                if (communities != null && !communities.isEmpty())
                {
                    community = communities.get(0);
                }
            }
        }
        switch (action)
        {
        
        case REMOVE:
            if (authService.getConfiguration().canItemAdminPerformBitstreamDeletion())
            {
                adminObject = item;
            }
            else if (authService.getConfiguration().canCollectionAdminPerformBitstreamDeletion())
            {
                adminObject = collection;
            }
            else if (authService.getConfiguration()
                    .canCommunityAdminPerformBitstreamDeletion())
            {
                adminObject = community;
            }
            break;
        case ADD:
            if (authService.getConfiguration().canItemAdminPerformBitstreamCreation())
            {
                adminObject = item;
            }
            else if (authService.getConfiguration()
                    .canCollectionAdminPerformBitstreamCreation())
            {
                adminObject = collection;
            }
            else if (authService.getConfiguration()
                    .canCommunityAdminPerformBitstreamCreation())
            {
                adminObject = community;
            }
            break;

        default:
            adminObject = this;
            break;
        }
        return adminObject;
    }

	@Transient
	public IDSpaceObject getParentObject()
    {
        List<Item> items = getItems();
       
        if (items != null && (!items.isEmpty() && items.get(0) != null))
        {
            return items.get(0);
        }
        else
        {
            return null;
        }
    }
}
