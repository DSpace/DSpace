/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna.value;

import java.sql.SQLException;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import it.cilea.osd.jdyna.model.AValue;

@Entity
@DiscriminatorValue(value="eperson")
public class EPersonValue extends AValue<EPerson>
{

    @ManyToOne
    @LazyCollection(LazyCollectionOption.TRUE)
    @JoinColumn(name="eperson")
    private EPerson real;

    @Override
    public EPerson getObject()
    {
        return real;
    }

    @Override
    protected void setReal(EPerson oggetto)
    {
        this.real = oggetto;
        if(oggetto != null) {
            sortValue = real.getFullName().toLowerCase();
        }   
    }

    @Override
    public EPerson getDefaultValue()
    {
        Context context = null;
        EPerson eperson = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            eperson = EPerson.create(context);
            context.restoreAuthSystemState();
        }
        catch (SQLException | AuthorizeException e)
        {
           //NONE
        }
        finally {            
            if(context!=null && context.isValid()) {
                context.abort();
            }
        }
        return eperson;
    }

    @Override
    public String[] getUntokenizedValue()
    {
        return getObject() != null?
                new String[]{String.valueOf(((EPerson)getObject()).getID())}:null;
    }

    @Override
    public String[] getTokenizedValue()
    {
        return null;
    }
    
 
}
