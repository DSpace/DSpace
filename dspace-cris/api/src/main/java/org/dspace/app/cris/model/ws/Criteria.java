/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.ws;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "cris_ws_criteria")
public class Criteria
{
    /** DB Primary key */
    @Id
    @GeneratedValue(generator = "CRIS_WS_CRITERIA_SEQ")
    @SequenceGenerator(name = "CRIS_WS_CRITERIA_SEQ", sequenceName = "CRIS_WS_CRITERIA_SEQ", allocationSize = 1)
    private Integer id;

    private boolean enabled;

    private String criteria;

    private String filter;

    public void setCriteria(String criteria)
    {
        this.criteria = criteria;
    }

    public String getCriteria()
    {
        return criteria;
    }

    public String getFilter()
    {
        return filter;
    }

    public void setFilter(String filter)
    {
        this.filter = filter;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public Integer getId()
    {
        return id;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isEnabled()
    {
        return enabled;
    }
}
