/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.supervision;

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

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.Group;
import org.dspace.supervision.service.SupervisionOrderService;

/**
 * Database entity representation of the supervision_orders table
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
@Entity
@Table(name = "supervision_orders")
public class SupervisionOrder implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "supervision_orders_seq")
    @SequenceGenerator(name = "supervision_orders_seq", sequenceName = "supervision_orders_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eperson_group_id")
    private Group group;

    /**
     * Protected constructor, create object using:
     * {@link SupervisionOrderService#create(Context, Item, Group)}
     */
    protected SupervisionOrder() {

    }

    @Override
    public Integer getID() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }
}
