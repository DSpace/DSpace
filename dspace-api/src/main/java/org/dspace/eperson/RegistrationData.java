/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.util.ArrayList;
import java.util.Date;
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

/**
 * Database entity representation of the registrationdata table
 *
 * @author kevinvandevelde at atmire.com
 */
@Entity
@Table(name = "registrationdata")
public class RegistrationData implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "registrationdata_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "registrationdata_seq")
    @SequenceGenerator(name = "registrationdata_seq", sequenceName = "registrationdata_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "email", unique = true, length = 64)
    private String email;

    @Column(name = "token", length = 48)
    private String token;

    @Column(name = "expires")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expires;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinTable(
         name = "registrationdata2group",
         joinColumns = {@JoinColumn(name = "registrationdata_id")},
         inverseJoinColumns = {@JoinColumn(name = "group_id")}
    )
    private final List<Group> groups = new ArrayList<Group>();

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.eperson.service.RegistrationDataService#create(Context)}
     */
    protected RegistrationData() {

    }

    public Integer getID() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    void setToken(String token) {
        this.token = token;
    }

    public Date getExpires() {
        return expires;
    }

    void setExpires(Date expires) {
        this.expires = expires;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void addGroup(Group group) {
        this.groups.add(group);
    }
}
