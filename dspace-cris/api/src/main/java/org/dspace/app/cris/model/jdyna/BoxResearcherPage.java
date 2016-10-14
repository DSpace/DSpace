/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import it.cilea.osd.jdyna.model.Containable;
import it.cilea.osd.jdyna.web.Box;
import it.cilea.osd.jdyna.web.ITabService;

@Entity
@Table(name = "cris_rp_box")
@org.hibernate.annotations.NamedQueries({
        @org.hibernate.annotations.NamedQuery(name = "BoxResearcherPage.findAll", query = "from BoxResearcherPage order by priority asc"),
        @org.hibernate.annotations.NamedQuery(name = "BoxResearcherPage.findContainableByHolder", query = "from Containable containable where containable in (select m from BoxResearcherPage box join box.mask m where box.id = ?)", cacheable = true),
        @org.hibernate.annotations.NamedQuery(name = "BoxResearcherPage.findHolderByContainable", query = "from BoxResearcherPage box where :par0 in elements(box.mask)", cacheable = true),
        @org.hibernate.annotations.NamedQuery(name = "BoxResearcherPage.uniqueBoxByShortName", query = "from BoxResearcherPage box where shortName = ?") })
public class BoxResearcherPage extends Box<Containable>
{

    @ManyToMany
    @JoinTable(name = "cris_rp_box2con", joinColumns = {
            @JoinColumn(name = "cris_rp_box_id") },
            inverseJoinColumns = { @JoinColumn(name = "jdyna_containable_id") })
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<Containable> mask;

    @ElementCollection
    @CollectionTable(
          name="cris_rp_box2policysingle",
          joinColumns=@JoinColumn(name="box_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<String> authorizedSingle;
    
    @ElementCollection
    @CollectionTable(
          name="cris_rp_box2policygroup",
          joinColumns=@JoinColumn(name="box_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<String> authorizedGroup;
    
    public BoxResearcherPage()
    {
        this.visibility = VisibilityTabConstant.ADMIN;
    }

    @Override
    public List<Containable> getMask()
    {
        if (this.mask == null)
        {
            this.mask = new LinkedList<Containable>();
        }
        return mask;
    }

    @Override
    public void setMask(List<Containable> mask)
    {
        if (mask != null)
        {
            Collections.sort(mask);
        }
        this.mask = mask;
    }


    @Override
    public List<String> getAuthorizedSingle()
    {
        return authorizedSingle;
    }

    @Override
    public void setAuthorizedSingle(List<String> authorizedSingle)
    {
        this.authorizedSingle = authorizedSingle; 
    }

    @Override
    public List<String> getAuthorizedGroup()
    {
        return authorizedGroup;
    }

    @Override
    public void setAuthorizedGroup(List<String> authorizedGroup)
    {
        this.authorizedGroup = authorizedGroup;
    }
    
    @Override
    public List<String> getMetadataWithPolicySingle(ITabService tabService)
    {        
        List<String> results = new ArrayList<String>();
        for(RPPropertiesDefinition pd : tabService.getAllPropertiesDefinitionWithPolicySingle(RPPropertiesDefinition.class)) {
            results.add(pd.getShortName());
        }
        return results;
    }

    @Override
    public List<String> getMetadataWithPolicyGroup(ITabService tabService)
    {
        List<String> results = new ArrayList<String>();
        for(RPPropertiesDefinition pd : tabService.getAllPropertiesDefinitionWithPolicyGroup(RPPropertiesDefinition.class)) {
            results.add(pd.getShortName());
        }
        return results;
    }

}
