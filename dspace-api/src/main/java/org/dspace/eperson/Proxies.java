/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

import javax.persistence.*;
import java.util.UUID;

/**
 * Database entity representation of the subscription table
 *
 * @author kevinvandevelde at atmire.com
 */
@Entity
@Table(name = "proxies")
public class Proxies implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "proxies_id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="proxies_seq")
    @SequenceGenerator(name="proxies_seq", sequenceName="proxies_seq", allocationSize = 1)
    private Integer id;


    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "depositor_id")

    @Column(name = "depositor_id", unique = false)
    private UUID ePerson_depositor;

    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "proxy_id")

    @Column(name = "proxy_id", unique = false)
    private UUID ePerson_proxy;

    @Column(name = "handle", unique = false)
    private String handle;

    @Column(name = "uuid", unique = false)
    private String uuid;


    /**
     * Protected constructor, create object using:
     * {@link org.dspace.eperson.service.SubscribeService#subscribe(Context, EPerson, Collection)}
     *
     */
    protected Proxies()
    {

    }

    public Integer getID() {
        return id;
    }


    public UUID getDepositor() {
        return ePerson_depositor;
    }

    void setDepositor(UUID ePerson_depositor) {
        this.ePerson_depositor = ePerson_depositor;
    }

    public UUID getProxy() {
        return ePerson_proxy;
    }

    void setProxy(UUID ePerson_proxy) {
        this.ePerson_proxy = ePerson_proxy;
    }

    public String getHandle() {
        return handle;
    }

    public String getUUID() {
        return uuid;
    }

    void setePerson(String handle) {
        this.handle = handle;
    }


}
