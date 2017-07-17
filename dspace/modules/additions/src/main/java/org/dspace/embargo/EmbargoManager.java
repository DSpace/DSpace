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

import javax.mail.MessagingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemDataset;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataSchema;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.PluginManager;
import org.dspace.handle.HandleManager;

import uk.ac.edina.datashare.utils.MetaDataUtil;

/**
 * Public interface to the embargo subsystem.
 * <p>
 * Configuration properties: (with examples)
 *   <br/># DC metadata field to hold the user-supplied embargo terms
 *   <br/>embargo.field.terms = dc.embargo.terms
 *   <br/># DC metadata field to hold computed "lift date" of embargo
 *   <br/>embargo.field.lift = dc.date.available
 *   <br/># String to indicate indefinite (forever) embargo in terms
 *   <br/>embargo.terms.open = Indefinite
 *   <br/># implementation of embargo setter plugin
 *   <br/>plugin.single.org.dspace.embargo.EmbargoSetter = edu.my.Setter
 *   <br/># implementation of embargo lifter plugin
 *   <br/>plugin.single.org.dspace.embargo.EmbargoLifter = edu.my.Lifter
 *
 * @author Larry Stone
 * @author Richard Rodgers
 */
public class EmbargoManager
{
    /** Special date signalling an Item is to be embargoed forever.
     ** The actual date is the first day of the year 10,000 UTC.
     **/
    public static final DCDate FOREVER = new DCDate("10000-01-01");

    /** log4j category */
    private static Logger log = Logger.getLogger(EmbargoManager.class);

    // Metadata field components for user-supplied embargo terms
    // set from the DSpace configuration by init()
    private static String terms_schema = null;
    private static String terms_element = null;
    private static String terms_qualifier = null;

    // Metadata field components for lift date, encoded as a DCDate
    // set from the DSpace configuration by init()
    private static String lift_schema = null;
    private static String lift_element = null;
    private static String lift_qualifier = null;

    // plugin implementations
    // set from the DSpace configuration by init()
    private static EmbargoSetter setter = null;
    private static EmbargoLifter lifter = null;

    /**
     * Put an Item under embargo until the specified lift date.
     * Calls EmbargoSetter plugin to adjust Item access control policies.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     */
    public static void setEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException
    {
        init();
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
        boolean ignoreAuth = context.ignoreAuthorization();
        try
        {
            context.setIgnoreAuthorization(true);
            item.clearMetadata(lift_schema, lift_element, lift_qualifier, Item.ANY);
            item.addMetadata(lift_schema, lift_element, lift_qualifier, null, slift);
            log.info("Set embargo on Item "+item.getHandle()+", expires on: "+slift);

            setter.setEmbargo(context, item);

            item.update();
        }
        finally
        {
            context.setIgnoreAuthorization(ignoreAuth);
        }
    }

    /**
     * Get the embargo lift date for an Item, if any.  This looks for the
     * metadata field configured to hold embargo terms, and gives it
     * to the EmbargoSetter plugin's method to interpret it into
     * an absolute timestamp.  This is intended to be called at the time
     * the Item is installed into the archive.
     * <p>
     * Note that the plugin is *always* called, in case it gets its cue for
     * the embargo date from sources other than, or in addition to, the
     * specified field.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     * @return lift date on which the embargo is to be lifted, or null if none
     */
    public static DCDate getEmbargoTermsAsDate(Context context, Item item)
        throws SQLException, AuthorizeException, IOException
    {
        init();
        Metadatum terms[] = item.getMetadata(terms_schema, terms_element,
                terms_qualifier, Item.ANY);

        DCDate result = null;

        // Its poor form to blindly use an object that could be null...
        if (terms == null)
            return null;

        result = setter.parseTerms(context, item,
                terms.length > 0 ? terms[0].value : null);

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

    /**
     * Lift the embargo on an item which is assumed to be under embargo.
     * Call the plugin to manage permissions in its own way, then delete
     * the administrative metadata fields that dictated embargo date.
     *
     * @param context the DSpace context
     * @param item the item on which to lift the embargo
     */
    public static void liftEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException
    {
        init();

        // new version of Embargo policies remain in place.
        //lifter.liftEmbargo(context, item);
        item.clearMetadata(lift_schema, lift_element, lift_qualifier, Item.ANY);

        // set the dc.date.available value to right now
        item.clearMetadata(MetadataSchema.DC_SCHEMA, "date", "available", Item.ANY);
        item.addMetadata(MetadataSchema.DC_SCHEMA, "date", "available", null, DCDate.getCurrent().toString());

        log.info("Lifting embargo on Item "+item.getHandle());
        item.update();
    }

    /**
     * Command-line service to scan for every Item with an expired embargo,
     * and then lift that embargo.
     * <p>
     * Options:
     * <dl>
     *   <dt>-c,--check</dt>
     *   <dd>         Function: ONLY check the state of embargoed Items, do
     *                      NOT lift any embargoes.</dd>
     *   <dt>-h,--help</dt>
     *   <dd>         Help.</dd>
     *   <dt>-i,--identifier</dt>
     *   <dd>         Process ONLY this Handle identifier(s), which must be
     *                      an Item.  Can be repeated.</dd>
     *   <dt>-l,--lift</dt>
     *   <dd>         Function: ONLY lift embargoes, do NOT check the state
     *                      of any embargoed Items.</dd>
     *   <dt>-n,--dryrun</dt>
     *   <dd>         Do not change anything in the data model; print
     *                      message instead.</dd>
     *   <dt>-v,--verbose</dt>
     *   <dd>         Print a line describing action taken for each
     *                      embargoed Item found.</dd>
     *   <dt>-q,--quiet</dt>
     *   <dd>         No output except upon error.</dd>
     * </dl>
     */
    public static void main(String argv[])
    {

        init();
        int status = 0;

        Options options = new Options();
        options.addOption("v", "verbose", false,
                "Print a line describing action taken for each embargoed Item found.");
        options.addOption("q", "quiet", false,
                "Do not print anything except for errors.");
        options.addOption("n", "dryrun", false,
                "Do not change anything in the data model, print message instead.");
        options.addOption("i", "identifier", true,
                        "Process ONLY this Handle identifier(s), which must be an Item.  Can be repeated.");
        options.addOption("c", "check", false,
                        "Function: ONLY check the state of embargoed Items, do NOT lift any embargoes.");
        options.addOption("l", "lift", false,
                        "Function: ONLY lift embargoes, do NOT check the state of any embargoed Items.");

        options.addOption("a", "adjust", false,
                "Function: Adjust bitstreams policies");

        // DATASHARE - start
        options.addOption("e", "expire", false, "Notify depositor an embargoed item is about to expire.");        
        // DATASHARE - end
        
        options.addOption("h", "help", false, "help");
        CommandLine line = null;
        try
        {
            line = new PosixParser().parse(options, argv);
        }
        catch(ParseException e)
        {
            System.err.println("Command error: " + e.getMessage());
            new HelpFormatter().printHelp(EmbargoManager.class.getName(), options);
            System.exit(1);
        }
        
        if (line.hasOption('h'))
        {
            new HelpFormatter().printHelp(EmbargoManager.class.getName(), options);
            System.exit(0);
        }

        // sanity check, --lift and --check are mutually exclusive:
        if (line.hasOption('l') && line.hasOption('c'))
        {
            System.err.println("Command error: --lift and --check are mutually exclusive, try --help for assistance.");
            System.exit(1);
        }

        Context context = null;
        try
        {
            context = new Context();
            context.setIgnoreAuthorization(true);
            Date now = new Date();
            
            // DATASHARE - start
            if (line.hasOption('e'))
            {
                //new HelpFormatter().printHelp(EmbargoManager.class.getName(), options);
                checkForExpiry(context);
                System.exit(0);
            }
                    
            // DATASHARE - end        

            // scan items under embargo
            if (line.hasOption('i'))
            {
                for (String handle : line.getOptionValues('i'))
                {
                    DSpaceObject dso = HandleManager.resolveToObject(context, handle);
                    if (dso == null)
                    {
                        System.err.println("Error, cannot resolve handle="+handle+" to a DSpace Item.");
                        status = 1;
                    }
                    else if (dso.getType() != Constants.ITEM)
                    {
                        System.err.println("Error, the handle="+handle+" is not a DSpace Item.");
                        status = 1;
                    }
                    else
                    {
                        if (processOneItem(context, (Item)dso, line, now))
                        {
                            status = 1;
                        }
                    }
                }
            }
            else
            {
                ItemIterator ii = Item.findByMetadataField(context, lift_schema, lift_element, lift_qualifier, Item.ANY);
                while (ii.hasNext())
                {
                    if (processOneItem(context, ii.next(), line, now))
                    {
                        status = 1;
                    }
                }
            }
            log.debug("Cache size at end = "+context.getCacheSize());
            context.complete();
            context = null;
        }
        catch (Exception e)
        {
            System.err.println("ERROR, got exception: "+e);
            e.printStackTrace();
            status = 1;
        }
        finally
        {
            if (context != null)
            {
                try
                {
                    context.abort();
                }
                catch (Exception e)
                {
                }
            }
        }
        System.exit(status);
    }

    // lift or check embargo on one Item, handle exceptions
    // return false on success, true if there was fatal exception.
    private static boolean processOneItem(Context context, Item item, CommandLine line, Date now)
        throws Exception
    {
        boolean status = false;
        Metadatum lift[] = item.getMetadata(lift_schema, lift_element, lift_qualifier, Item.ANY);

        if (lift.length > 0)
        {
            DCDate liftDate = new DCDate(lift[0].value);
            // need to survive any failure on a single item, go on to process the rest.
            try
            {
                if (line.hasOption('a')){
                    setter.setEmbargo(context, item);
                }
                else{
                    log.debug("Testing embargo on item="+item.getHandle()+", date="+liftDate.toString());
                    if (liftDate.toDate().before(now))
                    {
                        if (line.hasOption('v'))
                        {
                            System.err.println("Lifting embargo from Item handle=" + item.getHandle() + ", lift date=" + lift[0].value);
                        }
                        if (line.hasOption('n'))
                        {
                            if (!line.hasOption('q'))
                            {
                                System.err.println("DRY RUN: would have lifted embargo from Item handle=" + item.getHandle() + ", lift date=" + lift[0].value);
                            }
                        }
                        else if (!line.hasOption('c'))
                        {
                            liftEmbargo(context, item);

                            // DATASHARE - start
	                        context.commit();
                            Thread th = new ItemDataset(item).createDataset();
                            try{
                                th.join();
                            }
                            catch(InterruptedException ex){
                                System.out.println(ex);
                            }
                            // DATASHARE - end
                        }
                    }
                    else if (!line.hasOption('l'))
                    {
                        if (line.hasOption('v'))
                        {
                            System.err.println("Checking current embargo on Item handle=" + item.getHandle() + ", lift date=" + lift[0].value);
                        }
                        setter.checkEmbargo(context, item);
                    }
                }
            }
            catch (Exception e)
            {
                log.error("Failed attempting to lift embargo, item="+item.getHandle()+": ", e);
                System.err.println("Failed attempting to lift embargo, item="+item.getHandle()+": "+ e);
                status = true;
            }
        }
        context.removeCached(item, item.getID());
        return status;
    }



    // initialize - get plugins and MD field settings from config
    private static void init()
    {
        if (terms_schema == null)
        {
            String terms = ConfigurationManager.getProperty("embargo.field.terms");
            String lift = ConfigurationManager.getProperty("embargo.field.lift");
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

            setter = (EmbargoSetter)PluginManager.getSinglePlugin(EmbargoSetter.class);
            if (setter == null)
            {
                throw new IllegalStateException("The EmbargoSetter plugin was not defined in DSpace configuration.");
            }
            lifter = (EmbargoLifter)PluginManager.getSinglePlugin(EmbargoLifter.class);
            if (lifter == null)
            {
                throw new IllegalStateException("The EmbargoLifter plugin was not defined in DSpace configuration.");
            }
        }
    }

    // return the schema part of "schema.element.qualifier" metadata field spec
    private static String getSchemaOf(String field)
    {
        String sa[] = field.split("\\.", 3);
        return sa[0];
    }

    // return the element part of "schema.element.qualifier" metadata field spec, if any
    private static String getElementOf(String field)
    {
        String sa[] = field.split("\\.", 3);
        return sa.length > 1 ? sa[1] : null;
    }

    // return the qualifier part of "schema.element.qualifier" metadata field spec, if any
    private static String getQualifierOf(String field)
    {
        String sa[] = field.split("\\.", 3);
        return sa.length > 2 ? sa[2] : null;
    }

    // return the lift date assigned when embargo was set, or null, if either:
    // it was never under embargo, or the lift date has passed.
    private static DCDate recoverEmbargoDate(Item item) {
        DCDate liftDate = null;
        Metadatum lift[] = item.getMetadata(lift_schema, lift_element, lift_qualifier, Item.ANY);
        if (lift.length > 0)
        {
            liftDate = new DCDate(lift[0].value);
            // sanity check: do not allow an embargo lift date in the past.
            if (liftDate.toDate().before(new Date()))
            {
                liftDate = null;
            }
        }
        return liftDate;
    }

    // DATASHARE - start
    /**
     * Check for any items whose embargo is about to expire.
     * @param context
     */
    private static void checkForExpiry(Context context){
        try{
            Date now = new Date();
            ItemIterator ii = Item.findByMetadataField(context, lift_schema, lift_element, lift_qualifier, Item.ANY);
            while (ii.hasNext()){
                Item item = ii.next();
                Metadatum lift[] = item.getMetadata(lift_schema, lift_element, lift_qualifier, Item.ANY);

                if (lift.length > 0){
                    DCDate liftDate = new DCDate(lift[0].value);
                    
                    long diff = Math.abs(liftDate.toDate().getTime() - now.getTime());
                    long diffDays = diff / (24 * 60 * 60 * 1000);
                    
                    if(diffDays == 7){
                        String submitter = item.getSubmitter().getEmail();
                        String admin = ConfigurationManager.getProperty("mail.admin");
                        
                        if(!ConfigurationManager.getBooleanProperty("mail.server.disabled")){
                            Email mail = Email.getEmail(
                                    I18nUtil.getEmailFilename(context.getCurrentLocale(), "embargo_expire"));
                            mail.addArgument(MetaDataUtil.getTitle(item));
                            mail.addArgument(ConfigurationManager.getProperty("handle.canonical.prefix") + item.getHandle());
                            mail.addArgument(liftDate.displayDate(false, true, context.getCurrentLocale()));
                            
                            mail.addRecipient(submitter);
                            
                            if(!submitter.equals(admin)){
                                mail.addRecipient(admin);
                            }

                            try{
                                log.info("Sending email for embargo expiry message. Sent to " +
                                    submitter + ", " + admin + " for " + item.getHandle());
                                mail.send();
                            }
                            catch(MessagingException ex){
                                log.error("Problem sending embargo expiry message: " + ex);
                            }
                        }
                        else{
                            log.info("Mail sending is disabled. Embargo expiry message would have been sent to " +
                                    submitter + ", " + admin + " for " + item.getHandle());
                        }
                    }
                }
            }
        }
        catch(AuthorizeException ex){throw new RuntimeException(ex);}
        catch(SQLException ex){throw new RuntimeException(ex);}
        catch(IOException ex){throw new RuntimeException(ex);}
    }    
    // DATASHARE - end

}
