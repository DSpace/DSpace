/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.embargo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.service.PluginService;
import org.dspace.embargo.service.EmbargoService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Public interface to the embargo subsystem.
 * <p>
 * Configuration properties: (with examples)
 * {@code
 *   # DC metadata field to hold the user-supplied embargo terms
 *   embargo.field.terms = dc.embargo.terms
 *   # DC metadata field to hold computed "lift date" of embargo
 *   embargo.field.lift = dc.date.available
 *   # String to indicate indefinite (forever) embargo in terms
 *   embargo.terms.open = Indefinite
 *   # implementation of embargo setter plugin
 *   plugin.single.org.dspace.embargo.EmbargoSetter = edu.my.Setter
 *   # implementation of embargo lifter plugin
 *   plugin.single.org.dspace.embargo.EmbargoLifter = edu.my.Lifter
 * }
 * @author Larry Stone
 * @author Richard Rodgers
 */
public class EmbargoServiceImpl implements EmbargoService
{

    /** log4j category */
    private final Logger log = Logger.getLogger(EmbargoServiceImpl.class);

    // Metadata field components for user-supplied embargo terms
    // set from the DSpace configuration by init()
    protected String terms_schema = null;
    protected String terms_element = null;
    protected String terms_qualifier = null;

    // Metadata field components for lift date, encoded as a DCDate
    // set from the DSpace configuration by init()
    protected String lift_schema = null;
    protected String lift_element = null;
    protected String lift_qualifier = null;

    // plugin implementations
    // set from the DSpace configuration by init()
    protected EmbargoSetter setter = null;
    protected EmbargoLifter lifter = null;

    @Autowired(required = true)
    protected ItemService itemService;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected PluginService pluginService;

    protected EmbargoServiceImpl()
    {

    }

    @Override
    public void setEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException
    {
        // if lift is null, we might be restoring an item from an AIP
        DCDate myLift = getEmbargoTermsAsDate(context, item);
        if (myLift == null)
        {
             if ((myLift = recoverEmbargoDate(item)) == null)
             {
                 return;
             }
        }
        String slift = myLift.toString();
        try
        {
            context.turnOffAuthorisationSystem();
            itemService.clearMetadata(context, item, lift_schema, lift_element, lift_qualifier, Item.ANY);
            itemService.addMetadata(context, item, lift_schema, lift_element, lift_qualifier, null, slift);
            log.info("Set embargo on Item "+item.getHandle()+", expires on: "+slift);

            setter.setEmbargo(context, item);

            itemService.update(context, item);
        }
        finally
        {
            context.restoreAuthSystemState();
        }
    }

    @Override
    public DCDate getEmbargoTermsAsDate(Context context, Item item)
        throws SQLException, AuthorizeException
    {
        List<MetadataValue> terms = itemService.getMetadata(item, terms_schema, terms_element,
                terms_qualifier, Item.ANY);

        DCDate result = null;

        // Its poor form to blindly use an object that could be null...
        if (terms == null)
            return null;

        result = setter.parseTerms(context, item,
                terms.size() > 0 ? terms.get(0).getValue() : null);

        if (result == null)
            return null;

        // new DCDate(non-date String) means toDate() will return null
        Date liftDate = result.toDate();
        if (liftDate == null)
        {
            throw new IllegalArgumentException(
                    "Embargo lift date is uninterpretable:  "
                            + result.toString());
        }

        // sanity check: do not allow an embargo lift date in the past.
        if (liftDate.before(new Date()))
        {
            throw new IllegalArgumentException(
                    "Embargo lift date must be in the future, but this is in the past: "
                            + result.toString());
        }
        return result;
    }


    @Override
    public void liftEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException
    {
        // new version of Embargo policies remain in place.
        //lifter.liftEmbargo(context, item);
        itemService.clearMetadata(context, item, lift_schema, lift_element, lift_qualifier, Item.ANY);

        // set the dc.date.available value to right now
        itemService.clearMetadata(context, item, MetadataSchema.DC_SCHEMA, "date", "available", Item.ANY);
        itemService.addMetadata(context, item, MetadataSchema.DC_SCHEMA, "date", "available", null, DCDate.getCurrent().toString());

        log.info("Lifting embargo on Item "+item.getHandle());
        itemService.update(context, item);
    }


    /**
     * Initialize the bean (after dependency injection has already taken place).
     * Ensures the configurationService is injected, so that we can
     * get plugins and MD field settings from config.
     * Called by "init-method" in Spring config.
     */
    public void init() throws Exception
    {
        if (terms_schema == null)
        {
            String terms = configurationService.getProperty("embargo.field.terms");
            String lift = configurationService.getProperty("embargo.field.lift");
            if (terms == null || lift == null)
            {
                throw new IllegalStateException("Missing one or more of the required DSpace configuration properties for EmbargoManager, check your configuration file.");
            }
            terms_schema = getSchemaOf(terms);
            terms_element = getElementOf(terms);
            terms_qualifier = getQualifierOf(terms);
            lift_schema = getSchemaOf(lift);
            lift_element = getElementOf(lift);
            lift_qualifier = getQualifierOf(lift);

            setter = (EmbargoSetter)pluginService.getSinglePlugin(EmbargoSetter.class);
            if (setter == null)
            {
                throw new IllegalStateException("The EmbargoSetter plugin was not defined in DSpace configuration.");
            }
            lifter = (EmbargoLifter)pluginService.getSinglePlugin(EmbargoLifter.class);
            if (lifter == null)
            {
                throw new IllegalStateException("The EmbargoLifter plugin was not defined in DSpace configuration.");
            }
        }
    }

    // return the schema part of "schema.element.qualifier" metadata field spec
    protected String getSchemaOf(String field)
    {
        String sa[] = field.split("\\.", 3);
        return sa[0];
    }

    // return the element part of "schema.element.qualifier" metadata field spec, if any
    protected String getElementOf(String field)
    {
        String sa[] = field.split("\\.", 3);
        return sa.length > 1 ? sa[1] : null;
    }

    // return the qualifier part of "schema.element.qualifier" metadata field spec, if any
    protected String getQualifierOf(String field)
    {
        String sa[] = field.split("\\.", 3);
        return sa.length > 2 ? sa[2] : null;
    }

    // return the lift date assigned when embargo was set, or null, if either:
    // it was never under embargo, or the lift date has passed.
    protected DCDate recoverEmbargoDate(Item item) {
        DCDate liftDate = null;
        List<MetadataValue> lift = itemService.getMetadata(item, lift_schema, lift_element, lift_qualifier, Item.ANY);
        if (lift.size() > 0)
        {
            liftDate = new DCDate(lift.get(0).getValue());
            // sanity check: do not allow an embargo lift date in the past.
            if (liftDate.toDate().before(new Date()))
            {
                liftDate = null;
            }
        }
        return liftDate;
    }

    @Override
    public void checkEmbargo(Context context, Item item) throws SQLException, IOException, AuthorizeException {
        setter.checkEmbargo(context, item);
    }

    @Override
    public List<MetadataValue> getLiftMetadata(Context context, Item item)
    {
        return itemService.getMetadata(item, lift_schema, lift_element, lift_qualifier, Item.ANY);
    }

    @Override
    public Iterator<Item> findItemsByLiftMetadata(Context context) throws SQLException, IOException, AuthorizeException {
        return itemService.findByMetadataField(context, lift_schema, lift_element, lift_qualifier, Item.ANY);
    }
}
