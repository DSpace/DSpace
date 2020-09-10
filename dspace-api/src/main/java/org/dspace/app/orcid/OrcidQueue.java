/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.content.Item;
import org.dspace.core.ReloadableEntity;

@Entity
@Table(name = "orcid_queue")
public class OrcidQueue implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orcid_queue_id_seq")
    @SequenceGenerator(name = "orcid_queue_id_seq", sequenceName = "orcid_queue_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    protected Item owner;

    @ManyToOne
    @JoinColumn(name = "entity_id")
    private Item entity;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getID() {
        return getId();
    }

    public Item getOwner() {
        return owner;
    }

    public void setOwner(Item owner) {
        this.owner = owner;
    }

    public Item getEntity() {
        return entity;
    }

    public void setEntity(Item entity) {
        this.entity = entity;
    }

}
