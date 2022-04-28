/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

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

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

/**
 * Database entity representation of the subscription table
 *
 * @author kevinvandevelde at atmire.com
 */
@Entity
@Table(name = "subscription")
public class Subscription implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "subscription_id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscription_seq")
    @SequenceGenerator(name = "subscription_seq", sequenceName = "subscription_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eperson_id")
    private EPerson ePerson;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.eperson.service.SubscribeService#subscribe(Context, EPerson, Collection)}
     */
    protected Subscription() {

    }

    @Override
    public Integer getID() {
        return id;
    }

    public Collection getCollection() {
        return collection;
    }

    void setCollection(Collection collection) {
        this.collection = collection;
    }

    public EPerson getePerson() {
        return ePerson;
    }

    void setePerson(EPerson ePerson) {
        this.ePerson = ePerson;
    }
}
