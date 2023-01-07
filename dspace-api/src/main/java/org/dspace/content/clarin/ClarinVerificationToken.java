/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.clarin;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.core.ReloadableEntity;

/**
 * If the Shibboleth authentication failed because IdP hasn't sent the SHIB_EMAIL header.
 * The user retrieve the verification token to the email for registration and login.
 * In the case of the Shibboleth Auth failure the IdP headers are stored as the string into the `shib_headers` column.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Entity
@Table(name = "verification_token")
public class ClarinVerificationToken implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "verification_token_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "verification_token_verification_token_id_seq")
    @SequenceGenerator(name = "verification_token_verification_token_id_seq",
            sequenceName = "verification_token_verification_token_id_seq",
            allocationSize = 1)
    private Integer id;

    /**
     * Value of the Shibboleth `SHIB-NETID` header.
     */
    @Column(name = "eperson_netid")
    private String ePersonNetID = null;

    /**
     * The email filled in by the user.
     */
    @Column(name = "email")
    private String email = null;

    /**
     * In the case of the Shibboleth Auth failure the IdP headers are stored as the string into this column.
     */
    @Column(name = "shib_headers")
    private String shibHeaders = null;

    /**
     * Generated verification token which is sent to the email.
     */
    @Column(name = "token")
    private String token = null;

    public ClarinVerificationToken() {
    }

    public String getShibHeaders() {
        return shibHeaders;
    }

    public void setShibHeaders(String shibHeaders) {
        this.shibHeaders = shibHeaders;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getePersonNetID() {
        return ePersonNetID;
    }

    public void setePersonNetID(String ePersonNetID) {
        this.ePersonNetID = ePersonNetID;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public Integer getID() {
        return id;
    }
}
