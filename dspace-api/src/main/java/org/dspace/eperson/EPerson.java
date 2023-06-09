/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxyHelper;

/**
 * Class representing an e-person.
 *
 * @author David Stuve
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@Table(name = "eperson")
public class EPerson extends DSpaceObject implements DSpaceObjectLegacySupport {
    @Column(name = "eperson_id", insertable = false, updatable = false)
    private Integer legacyId;

    @Column(name = "netid", length = 64)
    private String netid;

    @Column(name = "last_active")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastActive;

    @Column(name = "can_log_in", nullable = true)
    private Boolean canLogIn;

    @Column(name = "email", unique = true, length = 64)
    private String email;

    @Column(name = "require_certificate")
    private boolean requireCertificate = false;

    @Column(name = "self_registered")
    private boolean selfRegistered = false;

    @Column(name = "password", length = 128)
    private String password;

    @Column(name = "salt", length = 32)
    private String salt;

    @Column(name = "session_salt", length = 32)
    private String sessionSalt;

    @Column(name = "digest_algorithm", length = 16)
    private String digestAlgorithm;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "epeople")
    private final List<Group> groups = new ArrayList<>();

    /**
     * The e-mail field (for sorting)
     */
    public static final int EMAIL = 1;

    /**
     * The last name (for sorting)
     */
    public static final int LASTNAME = 2;

    /**
     * The e-mail field (for sorting)
     */
    public static final int ID = 3;

    /**
     * The netid field (for sorting)
     */
    public static final int NETID = 4;

    /**
     * The e-mail field (for sorting)
     */
    public static final int LANGUAGE = 5;

    @Transient
    protected transient EPersonService ePersonService;

    @Transient
    private Date previousActive;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.eperson.service.EPersonService#create(Context)}
     */
    protected EPerson() {

    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    /**
     * Return true if this object equals obj, false otherwise.
     *
     * @param obj another EPerson.
     * @return true if EPerson objects are equal in ID, email, and full name
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
        if (getClass() != objClass) {
            return false;
        }
        final EPerson other = (EPerson) obj;
        if (!this.getID().equals(other.getID())) {
            return false;
        }
        if (!StringUtils.equals(this.getEmail(), other.getEmail())) {
            return false;
        }
        if (!StringUtils.equals(this.getFullName(), other.getFullName())) {
            return false;
        }
        return true;
    }

    /**
     * Return a hash code for this object.
     *
     * @return int hash of object
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + this.getID().hashCode();
        hash = 89 * hash + (this.getEmail() != null ? this.getEmail().hashCode() : 0);
        hash = 89 * hash + (this.getFullName() != null ? this.getFullName().hashCode() : 0);
        return hash;
    }

    /**
     * Get the e-person's language
     *
     * @return language code (or null if the column is an SQL NULL)
     */
    public String getLanguage() {
        return getePersonService().getMetadataFirstValue(this, "eperson", "language", null, Item.ANY);
    }

    /**
     * Set the EPerson's language.  Value is expected to be a Unix/POSIX
     * Locale specification of the form {language} or {language}_{territory},
     * e.g. "en", "en_US", "pt_BR" (the latter is Brazilian Portugese).
     *
     * @param context  The relevant DSpace Context.
     * @param language language code
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public void setLanguage(Context context, String language) throws SQLException {
        getePersonService().setMetadataSingleValue(context, this, "eperson", "language", null, null, language);
    }

    /**
     * Get the e-person's email address
     *
     * @return their email address (or null if the column is an SQL NULL)
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set the EPerson's email
     *
     * @param s the new email
     */
    public void setEmail(String s) {
        this.email = StringUtils.lowerCase(s);
        setModified();
    }

    /**
     * Get the e-person's netid
     *
     * @return their netid (DB constraints ensure it's never NULL)
     */
    public String getNetid() {
        return netid;
    }

    /**
     * Set the EPerson's netid
     *
     * @param netid the new netid
     */
    public void setNetid(String netid) {
        this.netid = netid;
        setModified();
    }

    /**
     * Get the e-person's full name, combining first and last name in a
     * displayable string.
     *
     * @return their full name (first + last name; if both are NULL, returns email)
     */
    public String getFullName() {
        String f = getFirstName();
        String l = getLastName();

        if ((l == null) && (f == null)) {
            return getEmail();
        } else if (f == null) {
            return l;
        } else {
            return (f + " " + l);
        }
    }

    /**
     * Get the eperson's first name.
     *
     * @return their first name (or null if the column is an SQL NULL)
     */
    public String getFirstName() {
        return getePersonService().getMetadataFirstValue(this, "eperson", "firstname", null, Item.ANY);
    }

    /**
     * Set the eperson's first name
     *
     * @param context   The relevant DSpace Context.
     * @param firstname the person's first name
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public void setFirstName(Context context, String firstname) throws SQLException {
        getePersonService().setMetadataSingleValue(context, this, "eperson", "firstname", null, null, firstname);
        setModified();
    }

    /**
     * Get the eperson's last name.
     *
     * @return their last name (or null if the column is an SQL NULL)
     */
    public String getLastName() {
        return getePersonService().getMetadataFirstValue(this, "eperson", "lastname", null, Item.ANY);
    }

    /**
     * Set the eperson's last name
     *
     * @param context  The relevant DSpace Context.
     * @param lastname the person's last name
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public void setLastName(Context context, String lastname) throws SQLException {
        getePersonService().setMetadataSingleValue(context, this, "eperson", "lastname", null, null, lastname);
        setModified();
    }

    /**
     * Indicate whether the user can log in
     *
     * @param login boolean yes/no
     */
    public void setCanLogIn(boolean login) {
        this.canLogIn = login;
        setModified();
    }

    /**
     * Can the user log in?
     *
     * @return boolean, yes/no
     */
    public boolean canLogIn() {
        return BooleanUtils.isTrue(canLogIn);
    }

    /**
     * Set require cert yes/no
     *
     * @param isrequired boolean yes/no
     */
    public void setRequireCertificate(boolean isrequired) {
        this.requireCertificate = isrequired;
        setModified();
    }

    /**
     * Get require certificate or not
     *
     * @return boolean, yes/no (or false if the column is an SQL NULL)
     */
    public boolean getRequireCertificate() {
        return requireCertificate;
    }

    /**
     * Indicate whether the user self-registered
     *
     * @param sr boolean yes/no
     */
    public void setSelfRegistered(boolean sr) {
        this.selfRegistered = sr;
        setModified();
    }

    /**
     * Is the user self-registered?
     *
     * @return boolean, yes/no (or false if the column is an SQL NULL)
     */
    public boolean getSelfRegistered() {
        return selfRegistered;
    }

    /**
     * Stamp the EPerson's last-active date.
     *
     * @param when latest activity timestamp, or null to clear.
     */
    public void setLastActive(Date when) {
        this.previousActive = lastActive;
        this.lastActive = when;
    }

    /**
     * Get the EPerson's last-active stamp.
     *
     * @return date when last logged on, or null.
     */
    public Date getLastActive() {
        return lastActive;
    }

    /**
     * @return type found in Constants, see {@link org.dspace.core.Constants#Constants Constants}
     */
    @Override
    public int getType() {
        return Constants.EPERSON;
    }

    @Override
    public String getName() {
        return getEmail();
    }

    String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    /**
     * Store the digest algorithm used to hash the password.  You should also
     * set the {@link setPassword password hash} and the
     * {@link setDigestAlgorithm digest algorithm}.
     *
     * @param digestAlgorithm
     */
    void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    String getSalt() {
        return salt;
    }

    /**
     * Store the salt used when hashing the password.  You should also set the
     * {@link setPassword password hash} and the {@link setDigestAlgorithm
     * digest algorithm}.
     *
     * @param salt
     */
    void setSalt(String salt) {
        this.salt = salt;
    }

    String getPassword() {
        return password;
    }

    /**
     * Store the <strong>hash of a</strong> password.  You should also set the
     * {@link setSalt salt} and the {@link setDigestAlgorithm digest algorithm}.
     *
     * @param password
     */
    void setPassword(String password) {
        this.password = password;
    }

    public List<Group> getGroups() {
        return groups;
    }

    private EPersonService getePersonService() {
        if (ePersonService == null) {
            ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        }
        return ePersonService;
    }

    public String getSessionSalt() {
        return sessionSalt;
    }

    public void setSessionSalt(String sessionSalt) {
        this.sessionSalt = sessionSalt;
    }

    public Date getPreviousActive() {
        if (previousActive == null) {
            return new Date(0);
        }
        return previousActive;
    }

    public boolean hasPasswordSet() {
        return StringUtils.isNotBlank(getPassword());
    }

}
