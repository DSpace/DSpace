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
import org.dspace.eperson.Group;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import it.cilea.osd.jdyna.model.AValue;

@Entity
@DiscriminatorValue(value="group")
public class GroupValue extends AValue<Group>
{

    @ManyToOne
    @LazyCollection(LazyCollectionOption.TRUE)
    @JoinColumn(name="group")
    private Group real;

    @Override
    public Group getObject()
    {
        return real;
    }

    @Override
    protected void setReal(Group oggetto)
    {
        this.real = oggetto;
        if(oggetto != null) {
            sortValue = real.getName().toLowerCase();
        }   
    }

    @Override
    public Group getDefaultValue()
    {
        Context context = null;
        Group eperson = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            eperson = Group.create(context);
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
                new String[]{String.valueOf(((Group)getObject()).getID())}:null;
    }

    @Override
    public String[] getTokenizedValue()
    {
        return null;
    }
    
 
}
