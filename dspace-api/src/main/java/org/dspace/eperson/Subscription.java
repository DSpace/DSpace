/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.content.DSpaceObject;
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dspace_object_id")
    private DSpaceObject dSpaceObject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eperson_id")
    private EPerson ePerson;

    /**
     * Represent subscription type, for example, "content" or  "statistics".
     * 
     * NOTE: Currently, in DSpace we use only one "content"
     */
    @Column(name = "type")
    private String subscriptionType;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.eperson.service.SubscribeService#subscribe(Context, EPerson, DSpaceObject, List, String)}
     */
    protected Subscription() {}

    @Override
    public Integer getID() {
        return id;
    }

    public DSpaceObject getDSpaceObject() {
        return this.dSpaceObject;
    }

    void setDSpaceObject(DSpaceObject dSpaceObject) {
        this.dSpaceObject = dSpaceObject;
    }

    public EPerson getEPerson() {
        return ePerson;
    }

    public void setEPerson(EPerson ePerson) {
        this.ePerson = ePerson;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public List<SubscriptionParameter> getSubscriptionParameterList() {
        return subscriptionParameterList;
    }

    public void setSubscriptionParameterList(List<SubscriptionParameter> subscriptionList) {
        this.subscriptionParameterList = subscriptionList;
    }

    public void addParameter(SubscriptionParameter subscriptionParameter) {
        subscriptionParameterList.add(subscriptionParameter);
        subscriptionParameter.setSubscription(this);
    }

    public void removeParameterList() {
        subscriptionParameterList.clear();
    }

    public void removeParameter(SubscriptionParameter subscriptionParameter) {
        subscriptionParameterList.remove(subscriptionParameter);
    }
}