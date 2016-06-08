/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Database entity representation of the registrationdata table
 *
 * @author kevinvandevelde at atmire.com
 */
@Entity
@Table(name="registrationdata")
public class RegistrationData implements ReloadableEntity<Integer> {

    @Id
    @Column(name="registrationdata_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="registrationdata_seq")
    @SequenceGenerator(name="registrationdata_seq", sequenceName="registrationdata_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "email", unique = true, length = 64)
    private String email;

    @Column(name = "token", length = 48)
    private String token;

    @Column(name = "expires")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expires;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.eperson.service.RegistrationDataService#create(Context)}
     *
     */
    protected RegistrationData()
    {

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
}
