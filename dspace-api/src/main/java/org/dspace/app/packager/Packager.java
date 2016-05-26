/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.packager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageIngester;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.workflow.WorkflowException;

/**
 * Command-line interface to the Packager plugin.
 * <p>
 * This class ONLY exists to provide a CLI for the packager plugins. It does not
 * "manage" the plugins and it is not called from within DSpace, but the name
 * follows a DSpace convention.
 * <p>
 * It can invoke one of the Submission (SIP) packagers to create a new DSpace
 * Item out of a package, or a Dissemination (DIP) packager to write an Item out
 * as a package.
 * <p>
 * Usage is as follows:<br>
 * (Add the -h option to get the command to show its own help)
 *
 * <pre>
 *  1. To submit a SIP  (submissions tend to create a *new* object, with a new handle.  If you want to restore an object, see -r option below)
 *   dspace packager
 *       -e {ePerson}
 *       -t {PackagerType}
 *       -p {parent-handle} [ -p {parent2} ...]
 *       [-o {name}={value} [ -o {name}={value} ..]]
 *       [-a] --- also recursively ingest all child packages of the initial package
 *                (child pkgs must be referenced from parent pkg)
 *       [-w]   --- skip Workflow
 *       {package-filename}
 *
 *   {PackagerType} must match one of the aliases of the chosen Packager
 *   plugin.
 *
 *   The &quot;-w&quot; option circumvents Workflow, and is optional.  The &quot;-o&quot;
 *   option, which may be repeated, passes options to the packager
 *   (e.g. &quot;metadataOnly&quot; to a DIP packager).
 *
 *  2. To restore an AIP  (similar to submit mode, but attempts to restore with the handles/parents specified in AIP):
 *   dspace packager
 *       -r     --- restores a object from a package info, including the specified handle (will throw an error if handle is already in use)
 *       -e {ePerson}
 *       -t {PackagerType}
 *       [-o {name}={value} [ -o {name}={value} ..]]
 *       [-a] --- also recursively restore all child packages of the initial package
 *                (child pkgs must be referenced from parent pkg)
 *       [-k]   --- Skip over errors where objects already exist and Keep Existing objects by default.
 *                  Use with -r to only restore objects which do not already exist.  By default, -r will throw an error
 *                  and rollback all changes when an object is found that already exists.
 *       [-f]   --- Force a restore (even if object already exists).
 *                  Use with -r to replace an existing object with one from a package (essentially a delete and restore).
 *                  By default, -r will throw an error and rollback all changes when an object is found that already exists.
 *       [-i {identifier-handle-of-object}] -- Optional when -f is specified.  When replacing an object, you can specify the
 *                  object to replace if it cannot be easily determined from the package itself.
 *       {package-filename}
 *
 *   Restoring is very similar to submitting, except that you are recreating pre-existing objects.  So, in a restore, the object(s) are
 *   being recreated based on the details in the AIP.  This means that the object is recreated with the same handle and same parent/children
 *   objects.  Not all {PackagerTypes} may support a "restore".
 *
 *  3. To write out a DIP:
 *   dspace packager
 *       -d
 *       -e {ePerson}
 *       -t {PackagerType}
 *       -i {identifier-handle-of-object}
 *       [-a] --- also recursively disseminate all child objects of this object
 *       [-o {name}={value} [ -o {name}={value} ..]]
 *       {package-filename}
 *
 *   The &quot;-d&quot; switch chooses a Dissemination packager, and is required.
 *   The &quot;-o&quot; option, which may be repeated, passes options to the packager
 *   (e.g. &quot;metadataOnly&quot; to a DIP packager).
 * </pre>
 *
 * Note that {package-filename} may be "-" for standard input or standard
 * output, respectively.
 *
 * @author Larry Stone
 * @author Tim Donohue
 * @version $Revision$
 */
public class Packager
{
    /* Various private global settings/options */
    protected String packageType = null;
    protected boolean submit = true;
    protected boolean userInteractionEnabled = true;

    // die from illegal command line
    protected static void usageError(String msg)
    {
        System.out.println(msg);
        System.out.println(" (run with -h flag for details)");
        System.exit(1);
    }

    public static void main(String[] argv) throws Exception
    {
        Options options = new Options();
        options.addOption("p", "parent", true,
                "Handle(s) of parent Community or Collection into which to ingest object (repeatable)");
        options.addOption("e", "eperson", true,
                "email address of eperson doing importing");
        options
                .addOption(
                        "w",
                        "install",
                        false,
                        "disable workflow; install immediately without going through collection's workflow");
        options.addOption("r", "restore", false, "ingest in \"restore\" mode.  Restores a missing object based on the contents in a package.");
        options.addOption("k", "keep-existing", false, "if an object is found to already exist during a restore (-r), then keep the existing object and continue processing.  Can only be used with '-r'.  This avoids object-exists errors which are thrown by -r by default.");
        options.addOption("f", "force-replace", false, "if an object is found to already exist during a restore (-r), then remove it and replace it with the contents of the package.  Can only be used with '-r'.  This REPLACES the object(s) in the repository with the contents from the package(s).");
        options.addOption("t", "type", true, "package type or MIMEtype");
        options
                .addOption("o", "option", true,
                        "Packager option to pass to plugin, \"name=value\" (repeatable)");
        options.addOption("d", "disseminate", false,
                "Disseminate package (output); default is to submit.");
        options.addOption("s", "submit", false,
                "Submission package (Input); this is the default. ");
        options.addOption("i", "identifier", true, "Handle of object to disseminate.");
        options.addOption("a", "all", false, "also recursively ingest/disseminate any child packages, e.g. all Items within a Collection (not all packagers may support this option!)");
        options.addOption("h", "help", false, "help (you may also specify '-h -t [type]' for additional help with a specific type of packager)");
        options.addOption("u", "no-user-interaction", false, "Skips over all user interaction (i.e. [y/n] question prompts) within this script. This flag can be used if you want to save (pipe) a report of all changes to a file, and therefore need to bypass all user interaction.");

        CommandLineParser parser = new PosixParser();
        CommandLine line = parser.parse(options, argv);

        String sourceFile = null;
        String eperson = null;
        String[] parents = null;
        String identifier = null;
        PackageParameters pkgParams = new PackageParameters();
        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

        //initialize a new packager -- we'll add all our current params as settings
        Packager myPackager = new Packager();

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("Packager  [options]  package-file|-\n",
                    options);
            //If user specified a type, also print out the SIP and DIP options
            // that are specific to that type of packager
            if (line.hasOption('t'))
            {
                System.out.println("\n--------------------------------------------------------------");
                System.out.println("Additional options for the " + line.getOptionValue('t') + " packager:");
                System.out.println("--------------------------------------------------------------");
                System.out.println("(These options may be specified using --option as described above)");

                PackageIngester sip = (PackageIngester) pluginService
                    .getNamedPlugin(PackageIngester.class, line.getOptionValue('t'));

                if (sip != null)
                {
                    System.out.println("\n\n" + line.getOptionValue('t') + " Submission (SIP) plugin options:\n");
                    System.out.println(sip.getParameterHelp());
                }
                else
                {
                    System.out.println("\nNo valid Submission plugin found for " + line.getOptionValue('t') + " type.");
                }

                PackageDisseminator dip = (PackageDisseminator) pluginService
                    .getNamedPlugin(PackageDisseminator.class, line.getOptionValue('t'));

                if (dip != null)
                {
                    System.out.println("\n\n" + line.getOptionValue('t') + " Dissemination (DIP) plugin options:\n");
                    System.out.println(dip.getParameterHelp());
                }
                else
                {
                    System.out.println("\nNo valid Dissemination plugin found for " + line.getOptionValue('t') + " type.");
                }

            }
            else  //otherwise, display list of valid packager types
            {
                System.out.println("\nAvailable Submission Package (SIP) types:");
                String pn[] = pluginService
                        .getAllPluginNames(PackageIngester.class);
                for (int i = 0; i < pn.length; ++i)
                {
                    System.out.println("  " + pn[i]);
                }
                System.out
                        .println("\nAvailable Dissemination Package (DIP) types:");
                pn = pluginService.getAllPluginNames(PackageDisseminator.class);
                for (int i = 0; i < pn.length; ++i)
                {
                    System.out.println("  " + pn[i]);
                }
            }
            System.exit(0);
        }

        //look for flag to disable all user interaction
        if(line.hasOption('u'))
        {
            myPackager.userInteractionEnabled = false;
        }
        if (line.hasOption('w'))
        {
            pkgParams.setWorkflowEnabled(false);
        }
        if (line.hasOption('r'))
        {
            pkgParams.setRestoreModeEnabled(true);
        }
        //keep-existing is only valid in restoreMode (-r) -- otherwise ignore -k option.
        if (line.hasOption('k') && pkgParams.restoreModeEnabled())
        {
            pkgParams.setKeepExistingModeEnabled(true);
        }
        //force-replace is only valid in restoreMode (-r) -- otherwise ignore -f option.
        if (line.hasOption('f') && pkgParams.restoreModeEnabled())
        {
            pkgParams.setReplaceModeEnabled(true);
        }
        if (line.hasOption('e'))
        {
            eperson = line.getOptionValue('e');
        }
        if (line.hasOption('p'))
        {
            parents = line.getOptionValues('p');
        }
        if (line.hasOption('t'))
        {
            myPackager.packageType = line.getOptionValue('t');
        }
        if (line.hasOption('i'))
        {
            identifier = line.getOptionValue('i');
        }
        if (line.hasOption('a'))
        {
            //enable 'recursiveMode' param to packager implementations, in case it helps with packaging or ingestion process
            pkgParams.setRecursiveModeEnabled(true);
        }
        String files[] = line.getArgs();
        if (files.length > 0)
        {
            sourceFile = files[0];
        }
        if (line.hasOption('d'))
        {
            myPackager.submit = false;
        }
        if (line.hasOption('o'))
        {
            String popt[] = line.getOptionValues('o');
            for (int i = 0; i < popt.length; ++i)
            {
                String pair[] = popt[i].split("\\=", 2);
                if (pair.length == 2)
                {
                    pkgParams.addProperty(pair[0].trim(), pair[1].trim());
                }
                else if (pair.length == 1)
                {
                    pkgParams.addProperty(pair[0].trim(), "");
                }
                else
                {
                    System.err
                            .println("Warning: Illegal package option format: \""
                                    + popt[i] + "\"");
                }
            }
        }

        // Sanity checks on arg list: required args
        // REQUIRED: sourceFile, ePerson (-e), packageType (-t)
        if (sourceFile == null || eperson == null || myPackager.packageType == null)
        {
            System.err.println("Error - missing a REQUIRED argument or option.\n");
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("PackageManager  [options]  package-file|-\n", options);
            System.exit(0);
        }

        // find the EPerson, assign to context
        Context context = new Context();
        EPerson myEPerson = null;
        myEPerson = EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, eperson);
        if (myEPerson == null)
        {
            usageError("Error, eperson cannot be found: " + eperson);
        }
        context.setCurrentUser(myEPerson);


        //If we are in REPLACE mode
        if(pkgParams.replaceModeEnabled())
        {
            PackageIngester sip = (PackageIngester) pluginService
                    .getNamedPlugin(PackageIngester.class, myPackager.packageType);
            if (sip == null)
            {
                usageError("Error, Unknown package type: " + myPackager.packageType);
            }

            DSpaceObject objToReplace = null;

            //if a specific identifier was specified, make sure it is valid
            if(identifier!=null && identifier.length()>0)
            {
                objToReplace = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, identifier);
                if (objToReplace == null)
                {
                    throw new IllegalArgumentException("Bad identifier/handle -- "
                            + "Cannot resolve handle \"" + identifier + "\"");
                }
            }

            String choiceString = null;
            if(myPackager.userInteractionEnabled)
            {
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("\n\nWARNING -- You are running the packager in REPLACE mode.");
                System.out.println("\nREPLACE mode may be potentially dangerous as it will automatically remove and replace contents within DSpace.");
                System.out.println("We highly recommend backing up all your DSpace contents (files & database) before continuing.");
                System.out.print("\nWould you like to continue? [y/n]: ");
                choiceString = input.readLine();
            }
            else
            {
                //user interaction disabled -- default answer to 'yes', otherwise script won't continue
                choiceString = "y";
            }

            if (choiceString.equalsIgnoreCase("y"))
            {
                System.out.println("Beginning replacement process...");

                try
                {
                    //replace the object from the source file
                    myPackager.replace(context, sip, pkgParams, sourceFile, objToReplace);

                    //commit all changes & exit successfully
                    context.complete();
                    System.exit(0);
                }
                catch (Exception e)
                {
                    // abort all operations
                    e.printStackTrace();
                    context.abort();
                    System.out.println(e);
                    System.exit(1);
                }
            }

        }
        //else if normal SUBMIT mode (or basic RESTORE mode -- which is a special type of submission)
        else if (myPackager.submit || pkgParams.restoreModeEnabled())
        {
            PackageIngester sip = (PackageIngester) pluginService
                    .getNamedPlugin(PackageIngester.class, myPackager.packageType);
            if (sip == null)
            {
                usageError("Error, Unknown package type: " + myPackager.packageType);
            }

            // validate each parent arg (if any)
            DSpaceObject parentObjs[] = null;
            if(parents!=null)
            {
                System.out.println("Destination parents:");

                parentObjs = new DSpaceObject[parents.length];
                for (int i = 0; i < parents.length; i++)
                {
                    // sanity check: did handle resolve?
                    parentObjs[i] = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context,
                            parents[i]);
                    if (parentObjs[i] == null)
                    {
                        throw new IllegalArgumentException(
                                "Bad parent list -- "
                                        + "Cannot resolve parent handle \""
                                        + parents[i] + "\"");
                    }
                    System.out.println((i == 0 ? "Owner: " : "Parent: ")
                            + parentObjs[i].getHandle());
                }
            }

            try
            {
                //ingest the object from the source file
                myPackager.ingest(context, sip, pkgParams, sourceFile, parentObjs);

                //commit all changes & exit successfully
                context.complete();
                System.exit(0);
            }
            catch (Exception e)
            {
                // abort all operations
                e.printStackTrace();
                context.abort();
                System.out.println(e);
                System.exit(1);
            }
        }// else, if DISSEMINATE mode
        else
        {
            //retrieve specified package disseminator
            PackageDisseminator dip = (PackageDisseminator) pluginService
                .getNamedPlugin(PackageDisseminator.class, myPackager.packageType);
            if (dip == null)
            {
                usageError("Error, Unknown package type: " + myPackager.packageType);
            }

            DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, identifier);
            if (dso == null)
            {
                throw new IllegalArgumentException("Bad identifier/handle -- "
                        + "Cannot resolve handle \"" + identifier + "\"");
            }

            //disseminate the requested object
            myPackager.disseminate(context, dip, dso, pkgParams, sourceFile);
        }
        System.exit(0);
    }

    /**
     * Ingest one or more DSpace objects from package(s) based on the
     * options passed to the 'packager' script.  This method is called
     * for both 'submit' (-s) and 'restore' (-r) modes.
     * <p>
     * Please note that replace (-r -f) mode calls the replace() method instead.
     *
     * @param context DSpace Context
     * @param sip PackageIngester which will actually ingest the package
     * @param pkgParams Parameters to pass to individual packager instances
     * @param sourceFile location of the source package to ingest
     * @param parentObjs Parent DSpace object(s) to attach new object to
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws FileNotFoundException if file doesn't exist
     * @throws AuthorizeException if authorization error
     * @throws CrosswalkException if crosswalk error
     * @throws PackageException if packaging error
     */
    protected void ingest(Context context, PackageIngester sip, PackageParameters pkgParams, String sourceFile, DSpaceObject parentObjs[])
            throws IOException, SQLException, FileNotFoundException, AuthorizeException, CrosswalkException, PackageException
    {
        // make sure we have an input file
        File pkgFile = new File(sourceFile);

        if(!pkgFile.exists())
        {
            System.out.println("\nERROR: Package located at " + sourceFile + " does not exist!");
            System.exit(1);
        }

        System.out.println("\nIngesting package located at " + sourceFile);

        //find first parent (if specified) -- this will be the "owner" of the object
        DSpaceObject parent = null;
        if(parentObjs!=null && parentObjs.length>0)
        {
            parent = parentObjs[0];
        }
        //NOTE: at this point, Parent may be null -- in which case it is up to the PackageIngester
        // to either determine the Parent (from package contents) or throw an error.

        try
        {
            //If we are doing a recursive ingest, call ingestAll()
            if(pkgParams.recursiveModeEnabled())
            {
                System.out.println("\nAlso ingesting all referenced packages (recursive mode)..");
                System.out.println("This may take a while, please check your logs for ongoing status while we process each package.");

                //ingest first package & recursively ingest anything else that package references (child packages, etc)
                List<String> hdlResults = sip.ingestAll(context, parent, pkgFile, pkgParams, null);

                if (hdlResults != null)
                {
                    //Report total objects created
                    System.out.println("\nCREATED a total of " + hdlResults.size() + " DSpace Objects.");

                    String choiceString = null;
                    //Ask if user wants full list printed to command line, as this may be rather long.
                    if (this.userInteractionEnabled)
                    {
                        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                        System.out.print("\nWould you like to view a list of all objects that were created? [y/n]: ");
                        choiceString = input.readLine();
                    }
                    else
                    {
                        // user interaction disabled -- default answer to 'yes', as
                        // we want to provide user with as detailed a report as possible.
                        choiceString = "y";
                    }

                    // Provide detailed report if user answered 'yes'
                    if (choiceString.equalsIgnoreCase("y"))
                    {
                        System.out.println("\n\n");
                        for (String result : hdlResults)
                        {
                            DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, result);

                            if(dso!=null)
                            {

                                if (pkgParams.restoreModeEnabled()) {
                                    System.out.println("RESTORED DSpace " + Constants.typeText[dso.getType()] +
                                            " [ hdl=" + dso.getHandle() + ", dbID=" + dso.getID() + " ] ");
                                } else {
                                    System.out.println("CREATED new DSpace " + Constants.typeText[dso.getType()] +
                                            " [ hdl=" + dso.getHandle() + ", dbID=" + dso.getID() + " ] ");
                                }
                            }
                        }
                    }
                }

            }
            else
            {
                //otherwise, just one package to ingest
                try
                {
                    DSpaceObject dso = sip.ingest(context, parent, pkgFile, pkgParams, null);

                    if (dso != null)
                    {
                        if (pkgParams.restoreModeEnabled())
                        {
                            System.out.println("RESTORED DSpace " + Constants.typeText[dso.getType()] +
                                    " [ hdl=" + dso.getHandle() + ", dbID=" + dso.getID() + " ] ");
                        }
                        else
                        {
                            System.out.println("CREATED new DSpace " + Constants.typeText[dso.getType()] +
                                    " [ hdl=" + dso.getHandle() + ", dbID=" + dso.getID() + " ] ");
                        }
                    }
                }
                catch (IllegalStateException ie)
                {
                    // NOTE: if we encounter an IllegalStateException, this means the
                    // handle is already in use and this object already exists.

                    //if we are skipping over (i.e. keeping) existing objects
                    if (pkgParams.keepExistingModeEnabled())
                    {
                        System.out.println("\nSKIPPED processing package '" + pkgFile + "', as an Object already exists with this handle.");
                    }
                    else // Pass this exception on -- which essentially causes a full rollback of all changes (this is the default)
                    {
                        throw ie;
                    }
                }
            }
        }
        catch (WorkflowException e)
        {
            throw new PackageException(e);
        }
    }


    /**
     * Disseminate one or more DSpace objects into package(s) based on the
     * options passed to the 'packager' script
     *
     * @param context DSpace context
     * @param dip PackageDisseminator which will actually create the package
     * @param dso DSpace Object to disseminate as a package
     * @param pkgParams Parameters to pass to individual packager instances
     * @param outputFile File where final package should be saved
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws FileNotFoundException if file doesn't exist
     * @throws AuthorizeException if authorization error
     * @throws CrosswalkException if crosswalk error
     * @throws PackageException if packaging error
     */
    protected void disseminate(Context context, PackageDisseminator dip,
			       DSpaceObject dso, PackageParameters pkgParams,
			       String outputFile)
            throws IOException, SQLException, FileNotFoundException, AuthorizeException, CrosswalkException, PackageException
    {
        // initialize output file
        File pkgFile = new File(outputFile);

        System.out.println("\nDisseminating DSpace " + Constants.typeText[dso.getType()] +
                            " [ hdl=" + dso.getHandle() + " ] to " + outputFile);

        //If we are doing a recursive dissemination of this object & all its child objects, call disseminateAll()
        if(pkgParams.recursiveModeEnabled())
        {
            System.out.println("\nAlso disseminating all child objects (recursive mode)..");
            System.out.println("This may take a while, please check your logs for ongoing status while we process each package.");

            //disseminate initial object & recursively disseminate all child objects as well
            List<File> fileResults = dip.disseminateAll(context, dso, pkgParams, pkgFile);

            if(fileResults!=null)
            {
                //Report total files created
                System.out.println("\nCREATED a total of " + fileResults.size() + " dissemination package files.");

                String choiceString = null;
                //Ask if user wants full list printed to command line, as this may be rather long.
                if(this.userInteractionEnabled)
                {
                    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                    System.out.print("\nWould you like to view a list of all files that were created? [y/n]: ");
                    choiceString = input.readLine();
                }
                else
                {
                    // user interaction disabled -- default answer to 'yes', as
                    // we want to provide user with as detailed a report as possible.
                    choiceString = "y";
                }

                // Provide detailed report if user answered 'yes'
                if (choiceString.equalsIgnoreCase("y"))
                {
                    System.out.println("\n\n");
                    for(File result : fileResults)
                    {
                        System.out.println("CREATED package file: " + result.getCanonicalPath());
                    }
                }
            }
        }
        else
        {
            //otherwise, just disseminate a single object to a single package file
            dip.disseminate(context, dso, pkgParams, pkgFile);

            if(pkgFile!=null && pkgFile.exists())
            {
                System.out.println("\nCREATED package file: " + pkgFile.getCanonicalPath());
            }
        }
    }



    /**
     * Replace an one or more existing DSpace objects with the contents of
     * specified package(s) based on the options passed to the 'packager' script.
     * This method is only called for full replaces ('-r -f' options specified)
     *
     * @param context DSpace Context
     * @param sip PackageIngester which will actually replace the object with the package
     * @param pkgParams Parameters to pass to individual packager instances
     * @param sourceFile location of the source package to ingest as the replacement
     * @param objToReplace DSpace object to replace (may be null if it will be specified in the package itself)
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws FileNotFoundException if file doesn't exist
     * @throws AuthorizeException if authorization error
     * @throws CrosswalkException if crosswalk error
     * @throws PackageException if packaging error
     */
    protected void replace(Context context, PackageIngester sip, PackageParameters pkgParams, String sourceFile, DSpaceObject objToReplace)
            throws IOException, SQLException, FileNotFoundException, AuthorizeException, CrosswalkException, PackageException
    {

        // make sure we have an input file
        File pkgFile = new File(sourceFile);

        if(!pkgFile.exists())
        {
            System.out.println("\nPackage located at " + sourceFile + " does not exist!");
            System.exit(1);
        }

        System.out.println("\nReplacing DSpace object(s) with package located at " + sourceFile);
        if(objToReplace!=null)
        {
            System.out.println("Will replace existing DSpace " + Constants.typeText[objToReplace.getType()] +
                            " [ hdl=" + objToReplace.getHandle() + " ]");
        }
        // NOTE: At this point, objToReplace may be null.  If it is null, it is up to the PackageIngester
        // to determine which Object needs to be replaced (based on the handle specified in the pkg, etc.)

        try
        {
            //If we are doing a recursive replace, call replaceAll()
            if (pkgParams.recursiveModeEnabled())
            {
                //ingest first object using package & recursively replace anything else that package references (child objects, etc)
                List<String> hdlResults = sip.replaceAll(context, objToReplace, pkgFile, pkgParams);

                if (hdlResults != null) {
                    //Report total objects replaced
                    System.out.println("\nREPLACED a total of " + hdlResults.size() + " DSpace Objects.");

                    String choiceString = null;
                    //Ask if user wants full list printed to command line, as this may be rather long.
                    if (this.userInteractionEnabled)
                    {
                        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                        System.out.print("\nWould you like to view a list of all objects that were replaced? [y/n]: ");
                        choiceString = input.readLine();
                    }
                    else
                    {
                        // user interaction disabled -- default answer to 'yes', as
                        // we want to provide user with as detailed a report as possible.
                        choiceString = "y";
                    }

                    // Provide detailed report if user answered 'yes'
                    if (choiceString.equalsIgnoreCase("y"))
                    {
                        System.out.println("\n\n");
                        for (String result : hdlResults)
                        {
                            DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, result);

                            if (dso != null)
                            {
                                System.out.println("REPLACED DSpace " + Constants.typeText[dso.getType()] +
                                        " [ hdl=" + dso.getHandle() + " ] ");
                            }
                        }
                    }


                }
            }
            else
            {
                //otherwise, just one object to replace
                DSpaceObject dso = sip.replace(context, objToReplace, pkgFile, pkgParams);

                if (dso != null)
                {
                    System.out.println("REPLACED DSpace " + Constants.typeText[dso.getType()] +
                            " [ hdl=" + dso.getHandle() + " ] ");
                }
            }
        }
        catch (WorkflowException e)
        {
            throw new PackageException(e);
        }
    }
}
