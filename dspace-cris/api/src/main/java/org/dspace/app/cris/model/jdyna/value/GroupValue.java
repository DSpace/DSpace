/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna.value;

import java.sql.SQLException;

import javax.persistence.Basic;
import javax.persistence.Column;
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
public class GroupValue extends AValue<Integer>
{

    @Basic
    @Column(name="customPointer")
    private Integer real;

    @Override
    public Integer getObject()
    {
        return real;
    }

    @Override
    protected void setReal(Integer oggetto)
    {
        this.real = oggetto;
        if(oggetto != null) {
            Context context = null;
            try {
                context = new Context();
                String displayValue = Group.find(context, oggetto).getName().toLowerCase();
                sortValue = displayValue.substring(0,(displayValue.length()<200?displayValue.length():200));
            }
            catch(Exception ex) {
                log.error(ex.getMessage(), ex);
            }
            finally {
                if(context!=null) {
                    context.abort();
                }
            }
        }   
    }

    @Override
    public Integer getDefaultValue()
    {
        Context context = null;
        Group group = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            group = Group.create(context);
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
        return group.getID();
    }

    @Override
    public String[] getUntokenizedValue()
    {
        return getObject() != null?
                new String[]{String.valueOf((getObject()))}:null;
    }

    @Override
    public String[] getTokenizedValue()
    {
        return null;
    }
    
 
}
