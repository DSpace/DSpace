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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.core.ReloadableEntity;

/**
 * Database entity representation of the subscription_parameter table
 * SubscriptionParameter represents a frequency with which an user wants to be notified.
 *
 * @author Alba Aliu at atis.al
 */
@Entity
@Table(name = "subscription_parameter")
public class SubscriptionParameter implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscription_parameter_seq")
    @SequenceGenerator(name = "subscription_parameter_seq", sequenceName = "subscription_parameter_seq",
                       allocationSize = 1)
    @Column(name = "subscription_parameter_id", unique = true)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    /*
     * Currently, we have only one use case for this attribute: "frequency"
     */
    @Column
    private String name;

    /*
     * Currently, we use this attribute only with following values: "D", "W", "M".
     * Where D stand for Day, W stand for Week and M stand for Month
     */
    @Column
    private String value;

    public SubscriptionParameter() {}

    public SubscriptionParameter(Integer id, Subscription subscription, String name, String value) {
        this.id = id;
        this.subscription = subscription;
        this.name = name;
        this.value = value;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public Integer getID() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}
