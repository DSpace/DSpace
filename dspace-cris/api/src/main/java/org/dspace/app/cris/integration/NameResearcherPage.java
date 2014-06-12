/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.util.Set;

/**
 * Support class to build the full list of names to process in the BindItemToRP
 * work method
 * 
 * @author cilea
 * 
 */
public class NameResearcherPage
{
    /** the name form to lookup for */
    private String name;

    /** the rp identifier */
    private String persistentIdentifier;

    private int id;

    /** the ids of previous rejected matches */
    private Set<Integer> rejectItems;

    public NameResearcherPage(String name, String authority, int id,
            Set<Integer> rejectItems)
    {
        this.name = name;
        this.persistentIdentifier = authority;
        this.id = id;
        this.rejectItems = rejectItems;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getPersistentIdentifier()
    {
        return persistentIdentifier;
    }

    public void setPersistentIdentifier(String persistentIdentifier)
    {
        this.persistentIdentifier = persistentIdentifier;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public Set<Integer> getRejectItems()
    {
        return rejectItems;
    }

    public void setRejectItems(Set<Integer> rejectItems)
    {
        this.rejectItems = rejectItems;
    }

}