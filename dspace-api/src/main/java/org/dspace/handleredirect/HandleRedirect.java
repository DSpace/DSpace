/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handleredirect;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.core.ReloadableEntity;


/**
 * Database entity representation of the handleredirect table
 *
 * @author Ying Jin at rice.edu
 */
@Entity
@Table(name = "handleredirect")
public class HandleRedirect implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "handle_redirect_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "handle_redirect_id_seq")
    @SequenceGenerator(name = "handle_redirect_id_seq", sequenceName = "handle_redirect_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "handle", unique = true)
    private String handle;

    @Column(name = "url", unique = true)
    private String url;

    protected HandleRedirect() {

    }

    @Override
    public Integer getID() {
        return id;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof HandleRedirect)) {
            return false;
        }

        HandleRedirect handle1 = (HandleRedirect) o;

        return new EqualsBuilder()
            .append(id, handle1.id)
            .append(handle, handle1.handle)
            .append(url, handle1.url)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(id)
            .append(handle)
            .append(url)
            .toHashCode();
    }
}
