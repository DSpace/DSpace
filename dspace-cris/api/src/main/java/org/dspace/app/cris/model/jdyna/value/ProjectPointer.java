/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna.value;

import it.cilea.osd.jdyna.value.PointerValue;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.dspace.app.cris.model.Project;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@DiscriminatorValue(value="projectpointer")
public class ProjectPointer extends PointerValue<Project>
{
    @ManyToOne
    @LazyCollection(LazyCollectionOption.TRUE)
    @JoinColumn(name="projectvalue")
    private Project real;
    
    @Override
    public Class<Project> getTargetClass()
    {
        return Project.class;
    }

    @Override
    public Project getObject()
    {
        return real;
    }

    @Override
    protected void setReal(Project oggetto)
    {
        this.real = oggetto;
        if(oggetto != null) {
            String displayValue = real.getDisplayValue();
            sortValue = displayValue.substring(0,(displayValue.length()<200?displayValue.length():200)).toLowerCase();
        }        
    }

    @Override
    public Project getDefaultValue()
    {       
        return null;
    }

}
