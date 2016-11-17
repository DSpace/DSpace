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

import org.dspace.app.cris.model.OrganizationUnit;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@DiscriminatorValue(value="oupointer")
public class OUPointer extends PointerValue<OrganizationUnit>
{

    @ManyToOne
    @LazyCollection(LazyCollectionOption.TRUE)
    @JoinColumn(name="ouvalue")
    private OrganizationUnit real;
    
    @Override
    public Class<OrganizationUnit> getTargetClass()
    {
        return OrganizationUnit.class;
    }

    @Override
    public OrganizationUnit getObject()
    {        
        return real;
    }

    @Override
    protected void setReal(OrganizationUnit oggetto)
    {
       this.real = oggetto;
       if(oggetto != null) {
           String displayValue = real.getDisplayValue();
           sortValue = displayValue.substring(0,(displayValue.length()<200?displayValue.length():200)).toLowerCase();
       }
    }

    @Override
    public OrganizationUnit getDefaultValue()
    {
        return null;
    }

    
}
