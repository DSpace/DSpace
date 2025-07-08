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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.PackagerFileService;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipTreeService;
import org.dspace.content.RelationshipType;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageException;
import org.dspace.content.packager.PackageIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageUtils;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;
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
 *  1. To submit a SIP  (submissions tend to create a *new* object, with a new handle.  If you want to restore an
 *  object, see -r option below)
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
 *       -r     --- restores a object from a package info, including the specified handle (will throw an error if
 *       handle is already in use)
 *       -e {ePerson}
 *       -t {PackagerType}
 *       [-o {name}={value} [ -o {name}={value} ..]]
 *       [-a] --- also recursively restore all child packages of the initial package
 *                (child pkgs must be referenced from parent pkg)
 *       [-k]   --- Skip over errors where objects already exist and Keep Existing objects by default.
 *                  Use with -r to only restore objects which do not already exist.  By default, -r will throw an error
 *                  and rollback all changes when an object is found that already exists.
 *       [-f]   --- Force a restore (even if object already exists).
 *                  Use with -r to replace an existing object with one from a package (essentially a delete and
 *                  restore).
 *                  By default, -r will throw an error and rollback all changes when an object is found that already
 *                  exists.
 *       [-i {identifier-handle-of-object}] -- Optional when -f is specified.  When replacing an object, you can
 *       specify the
 *                  object to replace if it cannot be easily determined from the package itself.
 *       {package-filename}
 *
 *   Restoring is very similar to submitting, except that you are recreating pre-existing objects.  So, in a restore,
 *   the object(s) are
 *   being recreated based on the details in the AIP.  This means that the object is recreated with the same handle
 *   and same parent/children
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
public class Packager {
    /* Various private global settings/options */
    protected String packageType = null;
    protected boolean submit = true;
    protected boolean userInteractionEnabled = true;
    protected Set<UUID> alreadyDissed = new TreeSet<>();

    // die from illegal command line
    protected static void usageError(String msg) {
        System.out.println(msg);
        System.out.println(" (run with -h flag for details)");
        System.exit(1);
    }

    public static void main(String[] argv) throws Exception {
        List<String> sourceFileIngest = new ArrayList<>();
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
        options.addOption("r", "restore", false,
                          "ingest in \"restore\" mode.  Restores a missing object based on the contents in a package.");
        options.addOption("k", "keep-existing", false,
                          "if an object is found to already exist during a restore (-r), then keep the existing " +
                              "object and continue processing.  Can only be used with '-r'.  This avoids " +
                              "object-exists errors which are thrown by -r by default.");
        options.addOption("f", "force-replace", false,
                          "if an object is found to already exist during a restore (-r), then remove it and replace " +
                              "it with the contents of the package.  Can only be used with '-r'.  This REPLACES the " +
                              "object(s) in the repository with the contents from the package(s).");
        options.addOption("t", "type", true, "package type or MIMEtype");
        options
            .addOption("o", "option", true,
                       "Packager option to pass to plugin, \"name=value\" (repeatable)");
        options.addOption("d", "disseminate", false,
                          "Disseminate package (output); default is to submit.");
        options.addOption("s", "submit", false,
                          "Submission package (Input); this is the default. ");
        options.addOption("i", "identifier", true, "Handle of object to disseminate.");
        options.addOption("a", "all", false,
                          "also recursively ingest/disseminate any child packages, e.g. all Items within a Collection" +
                              " (not all packagers may support this option!)");
        options.addOption("h", "help", false,
                          "help (you may also specify '-h -t [type]' for additional help with a specific type of " +
                              "packager)");
        options.addOption("z", "relationalScope", true,
                "The scope of relations to disseminate with parent item.");
        options.addOption("u", "no-user-interaction", false,
                          "Skips over all user interaction (i.e. [y/n] question prompts) within this script. This " +
                              "flag can be used if you want to save (pipe) a report of all changes to a file, and " +
                              "therefore need to bypass all user interaction.");
        options.addOption("y", "dryRun", false,
                "Dry run to output the result of an ingest without actually ingesting.");

        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse(options, argv);

        String eperson = null;
        String[] parents = null;
        String identifier = null;
        String relationalScope = null;
        boolean dryRun = false;
        PackageParameters pkgParams = new PackageParameters();
        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

        //initialize a new packager -- we'll add all our current params as settings
        Packager myPackager = new Packager();

        if (line.hasOption('h')) {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("Packager  [options]  package-file|-\n", options);
            //If user specified a type, also print out the SIP and DIP options
            // that are specific to that type of packager
            if (line.hasOption('t')) {
                System.out.println("\n--------------------------------------------------------------");
                System.out.println("Additional options for the " + line.getOptionValue('t') + " packager:");
                System.out.println("--------------------------------------------------------------");
                System.out.println("(These options may be specified using --option as described above)");

                PackageIngester sip = (PackageIngester) pluginService
                    .getNamedPlugin(PackageIngester.class, line.getOptionValue('t'));

                if (sip != null) {
                    System.out.println("\n\n" + line.getOptionValue('t') + " Submission (SIP) plugin options:\n");
                    System.out.println(sip.getParameterHelp());
                } else {
                    System.out.println("\nNo valid Submission plugin found for " + line.getOptionValue('t') + " type.");
                }

                PackageDisseminator dip = (PackageDisseminator) pluginService
                    .getNamedPlugin(PackageDisseminator.class, line.getOptionValue('t'));

                if (dip != null) {
                    System.out.println("\n\n" + line.getOptionValue('t') + " Dissemination (DIP) plugin options:\n");
                    System.out.println(dip.getParameterHelp());
                } else {
                    System.out.println("\nNo valid Dissemination plugin found for "
                            + line.getOptionValue('t') + " type.");
                }

            } else {
                //otherwise, display list of valid packager types
                System.out.println("\nAvailable Submission Package (SIP) types:");
                String pn[] = pluginService
                        .getAllPluginNames(PackageIngester.class);
                for (int i = 0; i < pn.length; ++i) {
                    System.out.println("  " + pn[i]);
                }
                System.out
                        .println("\nAvailable Dissemination Package (DIP) types:");
                pn = pluginService.getAllPluginNames(PackageDisseminator.class);
                for (int i = 0; i < pn.length; ++i) {
                    System.out.println("  " + pn[i]);
                }
            }
            System.exit(0);
        }

        //look for flag to disable all user interaction
        if (line.hasOption('u')) {
            myPackager.userInteractionEnabled = false;
        }
        if (line.hasOption('w')) {
            pkgParams.setWorkflowEnabled(false);
        }
        if (line.hasOption('r')) {
            pkgParams.setRestoreModeEnabled(true);
        }
        //keep-existing is only valid in restoreMode (-r) -- otherwise ignore -k option.
        if (line.hasOption('k') && pkgParams.restoreModeEnabled()) {
            pkgParams.setKeepExistingModeEnabled(true);
        }
        //force-replace is only valid in restoreMode (-r) -- otherwise ignore -f option.
        if (line.hasOption('f') && pkgParams.restoreModeEnabled()) {
            pkgParams.setReplaceModeEnabled(true);
        }
        if (line.hasOption('e')) {
            eperson = line.getOptionValue('e');
        }
        if (line.hasOption('p')) {
            parents = line.getOptionValues('p');
        }
        if (line.hasOption('t')) {
            myPackager.packageType = line.getOptionValue('t');
        }
        if (line.hasOption('i')) {
            identifier = line.getOptionValue('i');
        }
        if (line.hasOption("relationalScope")) {
            relationalScope = line.getOptionValue("relationalScope");
        }
        relationalScope = (relationalScope == null) ? "*" : relationalScope;
        pkgParams.setProperty("scope", relationalScope);
        if (line.hasOption("dryRun")) {
            dryRun = true;
        }
        if (line.hasOption('a')) {
            //enable 'recursiveMode' param to packager implementations, in case it helps with packaging or ingestion
            // process
            pkgParams.setRecursiveModeEnabled(true);
        }
        String[] sourceFiles = line.getArgs();
        if (line.hasOption('d')) {
            myPackager.submit = false;
        }
        if (line.hasOption('o')) {
            String popt[] = line.getOptionValues('o');
            for (int i = 0; i < popt.length; ++i) {
                String pair[] = popt[i].split("\\=", 2);
                if (pair.length == 2) {
                    pkgParams.addProperty(pair[0].trim(), pair[1].trim());
                } else if (pair.length == 1) {
                    pkgParams.addProperty(pair[0].trim(), "");
                } else {
                    System.err
                            .println("Warning: Illegal package option format: \""
                                    + popt[i] + "\"");
                }
            }
        }

        // Sanity checks on arg list: required args
        // REQUIRED: sourceFile, ePerson (-e), packageType (-t)
        if (sourceFiles == null || eperson == null || myPackager.packageType == null) {
            System.err.println("Error - missing a REQUIRED argument or option.\n");
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("PackageManager  [options]  package-file|-\n", options);
            System.exit(0);
        }

        // find the EPerson, assign to context
        Context context = new Context();
        EPerson myEPerson = null;
        myEPerson = EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, eperson);
        if (myEPerson == null) {
            usageError("Error, eperson cannot be found: " + eperson);
        }
        context.setCurrentUser(myEPerson);


        //If we are in REPLACE mode
        if (pkgParams.replaceModeEnabled()) {
            context.setMode(Context.Mode.BATCH_EDIT);
            PackageIngester sip = (PackageIngester) pluginService
                    .getNamedPlugin(PackageIngester.class, myPackager.packageType);
            if (sip == null) {
                usageError("Error, Unknown package type: " + myPackager.packageType);
            }

            DSpaceObject objToReplace = null;

            //if a specific identifier was specified, make sure it is valid
            if (identifier != null && identifier.length() > 0) {
                objToReplace = HandleServiceFactory.getInstance().getHandleService()
                                                   .resolveToObject(context, identifier);
                if (objToReplace == null) {
                    throw new IllegalArgumentException("Bad identifier/handle -- "
                                                           + "Cannot resolve handle \"" + identifier + "\"");
                }
            }

            String choiceString = null;
            if (myPackager.userInteractionEnabled) {
                if (dryRun) {
                    System.out.println("\n\n(DRYRUN MODE!)");
                }
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("\n\nWARNING -- You are running the packager in REPLACE mode.");
                System.out.println("\nREPLACE mode may be potentially dangerous as it will automatically" +
                        " remove and replace contents within DSpace.");
                System.out.println("We highly recommend backing up all your DSpace " +
                        "contents (files & database) before continuing.");
                System.out.print("\nWould you like to continue? [y/n]: ");
                choiceString = input.readLine();
            } else {
                //user interaction disabled -- default answer to 'yes', otherwise script won't continue
                choiceString = "y";
            }

            if (choiceString.equalsIgnoreCase("y")) {
                System.out.println("Beginning replacement process...");

                try {
                    //replace the object from the source file
                    dirAndFilePathBuilder(sourceFileIngest, sourceFiles);
                    if (identifier == null) {
                        String[] itemParts = sourceFileIngest.get(0).split("_ITEM@");
                        if (itemParts.length <= 1) {
                            myPackager.replace(context, sip, pkgParams, sourceFileIngest, objToReplace, dryRun);
                        } else {
                            String handle = itemParts[1].split(".zip")[0].replace('-', '/');
                            objToReplace = HandleServiceFactory.getInstance().getHandleService()
                                    .resolveToObject(context, handle);
                            myPackager.replace(context, sip, pkgParams, sourceFileIngest, objToReplace, dryRun);
                        }
                    } else {
                        myPackager.replace(context, sip, pkgParams, sourceFileIngest, objToReplace, dryRun);
                    }

                    //commit all changes & exit successfully
                    context.complete();
                    System.exit(0);
                } catch (Exception e) {
                    // abort all operations
                    e.printStackTrace();
                    context.abort();
                    System.out.println(e);
                    System.exit(1);
                }
            }

        } else if (myPackager.submit || pkgParams.restoreModeEnabled()) {
            //else if normal SUBMIT mode (or basic RESTORE mode -- which is a special type of submission)
            context.setMode(Context.Mode.BATCH_EDIT);

            PackageIngester sip = (PackageIngester) pluginService
                        .getNamedPlugin(PackageIngester.class, myPackager.packageType);
            if (sip == null) {
                usageError("Error, Unknown package type: " + myPackager.packageType);
            }

            // validate each parent arg (if any)
            DSpaceObject parentObjs[] = null;
            if (parents != null) {
                System.out.println("Destination parents:");

                parentObjs = new DSpaceObject[parents.length];
                for (int i = 0; i < parents.length; i++) {
                    // sanity check: did handle resolve?
                    parentObjs[i] = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context,
                                                                                                          parents[i]);
                    if (parentObjs[i] == null) {
                        throw new IllegalArgumentException(
                            "Bad parent list -- "
                                    + "Cannot resolve parent handle \""
                                    + parents[i] + "\"");
                    }
                    System.out.println((i == 0 ? "Owner: " : "Parent: ")
                                           + parentObjs[i].getHandle());
                }
            }

            try {
                //ingest the object from the source file
                dirAndFilePathBuilder(sourceFileIngest, sourceFiles);
                myPackager.ingest(context, sip, pkgParams, sourceFileIngest, parentObjs, dryRun, relationalScope);

                //commit all changes & exit successfully
                context.complete();
                System.exit(0);
            } catch (Exception e) {
                // abort all operations
                e.printStackTrace();
                context.abort();
                System.out.println(e);
                System.exit(1);
            }
        } else {
            // else, if DISSEMINATE mode
            context.setMode(Context.Mode.READ_ONLY);

            //retrieve specified package disseminator
            PackageDisseminator dip = (PackageDisseminator) pluginService
                        .getNamedPlugin(PackageDisseminator.class, myPackager.packageType);
            if (dip == null) {
                usageError("Error, Unknown package type: " + myPackager.packageType);
            }

            DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService()
                                                   .resolveToObject(context, identifier);
            if (dso == null) {
                throw new IllegalArgumentException("Bad identifier/handle -- "
                                                       + "Cannot resolve handle \"" + identifier + "\"");
            }

            //disseminate the requested object
            for (String sourceFile : sourceFiles) {
                myPackager.disseminate(context, dip, dso, pkgParams, sourceFile, relationalScope, dryRun);
            }
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
     * @param context    DSpace Context
     * @param sip        PackageIngester which will actually ingest the package
     * @param pkgParams  Parameters to pass to individual packager instances
     * @param sourceFiles locations of the source package(s) to ingest
     * @param parentObjs Parent DSpace object(s) to attach new object to
     * @throws IOException           if IO error
     * @throws SQLException          if database error
     * @throws FileNotFoundException if file doesn't exist
     * @throws AuthorizeException    if authorization error
     * @throws CrosswalkException    if crosswalk error
     * @throws PackageException      if packaging error
     */
    protected void ingest(Context context, PackageIngester sip, PackageParameters pkgParams, List<String> sourceFiles,
                          DSpaceObject parentObjs[], boolean dryRun, @Nullable String scope)
            throws IOException, SQLException, FileNotFoundException, AuthorizeException, CrosswalkException,
            PackageException {
        Map<String, String> pathToNewUUID = new HashMap<>();
        // make sure we have an input file
        PackagerFileService packagerFileService = new PackagerFileService(pkgParams);
        for (String sourceFileInit : sourceFiles) {
            //new list to contain newly minted UUIDs
            DSpaceObject dso = null;
            PackagerFileService.FileNode fileNode = null;
            if (dryRun) {
                dryRunIngest(context,sourceFileInit, scope, packagerFileService);
            } else {
                List<String> filePaths = new ArrayList<>();
                fileNode = packagerFileService.getFileNodeTree(context, sourceFileInit, scope).get(0);
                //List will NEVER be empty
                fileNode.getTreePaths(filePaths);
                for (String sourceFile : filePaths) {
                    //populate rels map
                    File pkgFile = new File(sourceFile);

                    if (!pkgFile.exists()) {
                        System.out.println("\nERROR: Package located at " + sourceFile + " does not exist!");
                        System.exit(1);
                    }

                    System.out.println("\nIngesting package located at " + sourceFile);

                    //find first parent (if specified) -- this will be the "owner" of the object
                    DSpaceObject parent = null;
                    if (parentObjs != null && parentObjs.length > 0) {
                        parent = parentObjs[0];
                    }
                    //NOTE: at this point, Parent may be null -- in which case it is up to the PackageIngester
                    // to either determine the Parent (from package contents) or throw an error.

                    try {
                        //If we are doing a recursive ingest, call ingestAll()
                        if (pkgParams.recursiveModeEnabled()) {
                            System.out.println("\nAlso ingesting all referenced packages (recursive mode)..");
                            System.out.println("This may take a while, please check " +
                                    "your logs for ongoing status while we process each package.");

                            //ingest first package & recursively ingest
                            // anything else that package references (child packages, etc)
                            List<String> hdlResults = sip.ingestAll(context, parent, pkgFile, pkgParams, null);

                            if (hdlResults != null) {
                                //Report total objects created
                                System.out.println("\nCREATED a total of " + hdlResults.size() + " DSpace Objects.");

                                String choiceString = null;
                                //Ask if user wants full list printed to command line, as this may be rather long.
                                if (this.userInteractionEnabled) {
                                    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                                    System.out.print("\nWould you like to view a list of all" +
                                            " objects that were created? [y/n]: ");
                                    choiceString = input.readLine();
                                } else {
                                    // user interaction disabled -- default answer to 'yes', as
                                    // we want to provide user with as detailed a report as possible.
                                    choiceString = "y";
                                }

                                // Provide detailed report if user answered 'yes'
                                if (choiceString.equalsIgnoreCase("y")) {
                                    System.out.println("\n\n");
                                    for (String result : hdlResults) {
                                        dso = HandleServiceFactory.getInstance().getHandleService()
                                                .resolveToObject(context, result);
                                        if (dso != null) {
                                            pathToNewUUID.put(sourceFile, dso.getID().toString());

                                            if (pkgParams.restoreModeEnabled()) {
                                                System.out.println("RESTORED DSpace " +
                                                        Constants.typeText[dso.getType()] + " [ hdl=" + dso.getHandle()
                                                        + ", dbID=" + dso.getID() + " ] ");
                                            } else {
                                                System.out.println("CREATED new DSpace " +
                                                        Constants.typeText[dso.getType()] + " [ hdl=" + dso.getHandle()
                                                        + ", dbID=" + dso.getID() + " ] ");
                                            }
                                        }
                                    }
                                }
                            }

                        } else {
                            //otherwise, just one package to ingest
                            try {
                                dso = sip.ingest(context, parent, pkgFile, pkgParams, null);
                                if (dso != null) {
                                    pathToNewUUID.put(sourceFile, dso.getID().toString());

                                    if (pkgParams.restoreModeEnabled()) {
                                        System.out.println("RESTORED DSpace " + Constants.typeText[dso.getType()] +
                                                " [ hdl=" + dso.getHandle() + ", dbID=" + dso.getID() + " ] ");
                                    } else {
                                        System.out.println("CREATED new DSpace " + Constants.typeText[dso.getType()] +
                                                " [ hdl=" + dso.getHandle() + ", dbID=" + dso.getID() + " ] ");
                                    }
                                }
                            } catch (IllegalStateException ie) {
                                // NOTE: if we encounter an IllegalStateException, this means the
                                // handle is already in use and this object already exists.

                                //if we are skipping over (i.e. keeping) existing objects
                                if (pkgParams.keepExistingModeEnabled()) {
                                    System.out.println(
                                            "\nSKIPPED processing package '" + pkgFile +
                                                    "', as an Object already exists with this handle.");
                                } else {
                                    // Pass this exception on -- which essentially causes
                                    // a full rollback of all changes (thisis the default)
                                    throw ie;
                                }
                            }
                        }
                    } catch (WorkflowException e) {
                        throw new PackageException(e);
                    }
                }
            }
        }
        if (pkgParams.recursiveModeEnabled()) {
            pathToNewUUID = sip.getPathToNewUUID();
        }
        if (!dryRun) {
            for (String path : pathToNewUUID.keySet()) {
                if (getDSOTypeFromUUID(context, pathToNewUUID.get(path)) == Constants.ITEM) {
                    PackagerFileService.FileNode fileNode = packagerFileService
                            .getFileNodeTree(context, path, scope).get(0);
                    Map<String, Map<String, Map<String, Map<Integer, Integer>>>> pathToRelMap =
                            fileNode.getPathToRelMap();
                    Map<String, Map<String, Map<Integer, Integer>>> relToUUID = new HashMap<>();
                    Map<String, Map<Integer, Integer>> paths = new HashMap<>();
                    if (pathToRelMap.size() > 0) {
                        for (String relation : pathToRelMap.get(path).keySet()) {
                            paths = pathToRelMap.get(path).get(relation);
                            Map<String, Map<Integer, Integer>> uuids = new HashMap<>();
                            for (String relPath : paths.keySet()) {
                                //Check for varrying paths but file NAMES should line up
                                if (pathToNewUUID.get(relPath) == null) {
                                    for (String pathCheck : pathToNewUUID.keySet()) {
                                        if (pathCheck.contains(relPath)) {
                                            uuids.put(pathToNewUUID.get(pathCheck), paths.get(relPath));
                                            break;
                                        }
                                    }
                                } else {
                                    uuids.put(pathToNewUUID.get(relPath), paths.get(relPath));
                                }
                            }
                            relToUUID.put(relation, uuids);
                            addRelationships(context, relToUUID, pathToNewUUID.get(path));
                        }
                    }
                }
            }
        }
    }


    /**
     * Disseminate one or more DSpace objects into package(s) based on the
     * options passed to the 'packager' script
     *
     * @param context    DSpace context
     * @param dip        PackageDisseminator which will actually create the package
     * @param dsoParent        DSpace Object to disseminate as a package
     * @param pkgParams  Parameters to pass to individual packager instances
     * @param outputFile File where final package should be saved
     * @throws IOException           if IO error
     * @throws SQLException          if database error
     * @throws FileNotFoundException if file doesn't exist
     * @throws AuthorizeException    if authorization error
     * @throws CrosswalkException    if crosswalk error
     * @throws PackageException      if packaging error
     */
    protected void disseminate(Context context, PackageDisseminator dip,
                               DSpaceObject dsoParent, PackageParameters pkgParams,
                               String outputFile, @Nullable String relationalScope, boolean dryRun )
            throws IOException, SQLException, FileNotFoundException, AuthorizeException, CrosswalkException,
            PackageException {
        ServiceManager serviceManager = new DSpace().getServiceManager();
        RelationshipTreeService treeService = serviceManager.getServiceByName(
                RelationshipTreeService.class.getName(), RelationshipTreeService.class);
        DSpaceObjectService dSpaceObjectService = ContentServiceFactory.getInstance().getDSpaceObjectService(dsoParent);
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        //List of UUIDs to disseminate
        ArrayList<DSpaceObject> dsoToProcess = new ArrayList<>();
        dsoToProcess.add(dsoParent);

        List<DSpaceObject> alreadyDissedDSOs = new ArrayList<>();
        String orginalOutputPath = outputFile;
        if (dryRun) {
            Set<UUID> relatedUUIDSet = new TreeSet<>();
            System.out.println("DRYRUN Listing UUIDs of DSpace Objects to be disseminated");
            for (DSpaceObject dso : dsoToProcess) {
                relatedUUIDSet.add(dso.getID());
                if (dso.getType() == 2) {
                    Item item = (Item) dso;
                    relatedUUIDSet = treeService.getItemsInTree(context, item, relationalScope, false);
                    dryRunDisseminate(relatedUUIDSet);
                }
            }
            System.out.println("Total DSpace Objects: " + relatedUUIDSet.size());
        } else {
            for (DSpaceObject dso : dsoToProcess) {
                // initialize output file
                outputFile = orginalOutputPath;
                String dsoType = Constants.typeText[dso.getType()];
                String extension = getMIMEType(pkgParams).split("/")[1];
                String fileName = "";
                fileName = evalFileName(fileName, extension, dsoType, dso);
                outputFile = outputFile.replaceAll(".([^.]*)$", "_" + fileName);
                File pkgFile = new File(outputFile);
                System.out.println("\nDisseminating DSpace " + Constants.typeText[dso.getType()] +
                        " [ hdl=" + dso.getHandle() + " ] to " + outputFile);

                //If we are doing a recursive dissemination of this object
                // & all its child objects, call disseminateAll()
                if (pkgParams.recursiveModeEnabled()) {
                    System.out.println("\nAlso disseminating all child objects (recursive mode)..");
                    System.out.println("This may take a while, please check your logs " +
                            "for ongoing status while we process each package.");

                    //disseminate initial object & recursively disseminate all child objects as well
                    String pkgDirectory = pkgFile.getCanonicalFile().getParent();
                    List<File> fileResults = dip.disseminateAll(context, dso, pkgParams, pkgFile, alreadyDissedDSOs);
                    //Build set of UUIDs to process
                    Set<UUID> relatedUUIDSet = new TreeSet<>();
                    for (DSpaceObject childDSO : alreadyDissedDSOs) {
                        //Only items make it into alreadyDissedDSOs
                        //Diss all related items as well
                        Item childItem = (Item) childDSO;
                        relatedUUIDSet.addAll(treeService
                                .getItemsInTree(context, childItem, relationalScope, false));
                    }
                    //After we build related UUID set, process them
                    for (UUID uuid : relatedUUIDSet) {
                        if (!alreadyDissed.contains(uuid)) {
                            Item childOfChildItem = itemService.find(context, uuid);
                            fileName = pkgDirectory + "/" + PackageUtils.getPackageName(childOfChildItem, extension);
                            pkgFile = new File(fileName);
                            dip.disseminate(context, childOfChildItem, pkgParams, pkgFile);
                            if (fileResults != null) {
                                fileResults.add(pkgFile);
                            }
                        }
                    }

                    if (fileResults != null) {
                        //Report total files created
                        System.out.println("\nCREATED a total of " + fileResults.size() +
                                " dissemination package files.");

                        String choiceString = null;
                        //Ask if user wants full list printed to command line, as this may be rather long.
                        if (this.userInteractionEnabled) {
                            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                            System.out.print("\nWould you like to view a list of all files that were created? [y/n]: ");
                            choiceString = input.readLine();
                        } else {
                            // user interaction disabled -- default answer to 'yes', as
                            // we want to provide user with as detailed a report as possible.
                            choiceString = "y";
                        }

                        // Provide detailed report if user answered 'yes'
                        if (choiceString.equalsIgnoreCase("y")) {
                            System.out.println("\n\n");
                            for (File result : fileResults) {
                                System.out.println("CREATED package file: " + result.getCanonicalPath());
                            }
                        }
                    }
                } else {
                    //otherwise, just disseminate a single object to a single package file
                    dip.disseminate(context, dso, pkgParams, pkgFile);
                    alreadyDissed.add(dso.getID());

                    if (pkgFile.exists()) {
                        System.out.println("\nCREATED package file: " + pkgFile.getCanonicalPath());
                    }
                    //Diss all related items as well
                    if (dso.getType() == 2) {
                        Item parentItem = (Item) dso;
                        Set<UUID> relatedUUIDSet = treeService
                                .getItemsInTree(context, parentItem, relationalScope, false);
                        for (UUID uuid : relatedUUIDSet) {
                            if (!alreadyDissed.contains(uuid)) {
                                disseminate(context, dip, dSpaceObjectService
                                        .find(context, uuid), pkgParams, orginalOutputPath, relationalScope, dryRun);
                            }
                        }
                    }
                }
            }
        }
    }

    //Short hand fileName utill
    public String evalFileName(String fileName, String extension, String dsoType, DSpaceObject dso) {
        if (extension.equalsIgnoreCase("zip")) {
            fileName = dsoType + "@" + dso.getHandle().replace('/', '-') + "." + extension;
        } else {
            fileName = dsoType + "@" + dso.getHandle().replace('/', '-') + "/manifest." + extension;
        }
        return fileName;
    }

    /**
     * Replace an one or more existing DSpace objects with the contents of
     * specified package(s) based on the options passed to the 'packager' script.
     * This method is only called for full replaces ('-r -f' options specified)
     *
     * @param context      DSpace Context
     * @param sip          PackageIngester which will actually replace the object with the package
     * @param pkgParams    Parameters to pass to individual packager instances
     * @param sourceFiles   location of the source package to ingest as the replacement
     * @param objToReplace DSpace object to replace (may be null if it will be specified in the package itself)
     * @throws IOException           if IO error
     * @throws SQLException          if database error
     * @throws FileNotFoundException if file doesn't exist
     * @throws AuthorizeException    if authorization error
     * @throws CrosswalkException    if crosswalk error
     * @throws PackageException      if packaging error
     */
    protected void replace(Context context, PackageIngester sip, PackageParameters pkgParams,  List<String> sourceFiles,
                           DSpaceObject objToReplace, boolean dryRun)
            throws IOException, SQLException, FileNotFoundException, AuthorizeException, CrosswalkException,
            PackageException {

        PackagerFileService packagerFileService = new PackagerFileService(pkgParams);
        for (String sourceFileInit : sourceFiles) {
            if (dryRun) {
                dryRunIngest(context,sourceFileInit, pkgParams.getProperty("scope"), packagerFileService);
            } else {
                List<String> filePaths = new ArrayList<>();
                List<PackagerFileService.FileNode> fileNodes = packagerFileService
                        .getFileNodeTree(context, sourceFileInit, pkgParams.getProperty("scope"));
                //List will NEVER be empty
                fileNodes.get(0).getTreePaths(filePaths);
                for (String sourceFile : filePaths) {
                    //populate rels map
                    File pkgFile = new File(sourceFile);

                    if (!pkgFile.exists()) {
                        System.out.println("\nPackage located at " + sourceFile + " does not exist!");
                        System.exit(1);
                    }

                    System.out.println("\nReplacing DSpace object(s) with package located at " + sourceFile);
                    if (objToReplace != null) {
                        System.out.println("Will replace existing DSpace " +
                                Constants.typeText[objToReplace.getType()] +
                                " [ hdl=" + objToReplace.getHandle() + " ]");
                    }
                    // NOTE: At this point, objToReplace may be null.  If it is null, it is up to the PackageIngester
                    // to determine which Object needs to be replaced (based on the handle specified in the pkg, etc.)

                    try {
                        //If we are doing a recursive replace, call replaceAll()
                        if (pkgParams.recursiveModeEnabled()) {
                            //ingest first object using package & recursively
                            // replace anything else that package references(child objects, etc)
                            List<String> hdlResults = sip.replaceAll(context, objToReplace, pkgFile, pkgParams);

                            if (hdlResults != null) {
                                //Report total objects replaced
                                System.out.println("\nREPLACED a total of " + hdlResults.size() + " DSpace Objects.");

                                String choiceString = null;
                                //Ask if user wants full list printed to command line, as this may be rather long.
                                if (this.userInteractionEnabled) {
                                    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                                    System.out.print("\nWould you like to view a list of all objects " +
                                            "that were replaced? [y/n]: ");
                                    choiceString = input.readLine();
                                } else {
                                    // user interaction disabled -- default answer to 'yes', as
                                    // we want to provide user with as detailed a report as possible.
                                    choiceString = "y";
                                }

                                // Provide detailed report if user answered 'yes'
                                if (choiceString.equalsIgnoreCase("y")) {
                                    System.out.println("\n\n");
                                    for (String result : hdlResults) {
                                        DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService()
                                                .resolveToObject(context, result);

                                        if (dso != null) {
                                            System.out.println("REPLACED DSpace " + Constants.typeText[dso.getType()] +
                                                    " [ hdl=" + dso.getHandle() + " ] ");
                                        }
                                    }
                                }


                            }
                        } else {
                            //otherwise, just one object to replace
                            if (objToReplace != null) {
                                String[] itemParts = pkgFile.getAbsolutePath().split("_ITEM@");
                                if (itemParts.length > 1) {
                                    String handle = itemParts[1].split(".zip")[0].replace('-', '/');
                                    if (!Objects.equals(objToReplace.getHandle(), handle)) {
                                        objToReplace = HandleServiceFactory.getInstance().getHandleService()
                                                .resolveToObject(context, handle);
                                        if (objToReplace == null) {
                                            throw new IllegalArgumentException("Bad identifier/handle -- "
                                                    + "Cannot resolve handle \"" + handle + "\"");
                                        }
                                    }
                                }
                            }
                            DSpaceObject dso = sip.replace(context, objToReplace, pkgFile, pkgParams);

                            if (dso != null) {
                                System.out.println("REPLACED DSpace " + Constants.typeText[dso.getType()] +
                                        " [ hdl=" + dso.getHandle() + " ] ");
                            }
                        }
                    } catch (WorkflowException e) {
                        throw new PackageException(e);
                    }
                }
            }
        }
        if (!dryRun) {
            for (String sourceFile : sourceFiles) {
                PackagerFileService.FileNode fileNode = packagerFileService
                        .getFileNodeTree(context, sourceFile, pkgParams.getProperty("scope")).get(0);
                Map<String, List<String>> parentRelationshipMap = new HashMap<>();
                if (!parentRelationshipMap.isEmpty()) {
                    //addRelationships(context, fileNode.getPathToRelMap(), fileNode.uuid);
                }
            }
        }
    }

    public void addRelationships(Context context, Map<String, Map<String, Map<Integer, Integer>>> relsMap,
                                 String initUUID) throws SQLException, AuthorizeException {
        //All IDs at this point should resolve as Items in the DB
        //We're adding relations ships TO `initUUID as Item` derived FROM the relsMap
        RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        RelationshipTypeService relationshipTypeService = ContentServiceFactory
                .getInstance().getRelationshipTypeService();
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        Item parentItem = itemService.find(context, UUID.fromString(initUUID));
        String parentEntityTypeLabel = itemService
                .getMetadataFirstValue(parentItem, "dspace", "entity", "type", null);
        for (String key : relsMap.keySet()) {
            //This list will be in place order with respect to relationship side
            for (String childUUIDString : relsMap.get(key).keySet()) {
                Item childItem = itemService.find(context, UUID.fromString(childUUIDString));
                String childEntityTypeLabel = itemService.getMetadataFirstValue(
                        childItem, "dspace", "entity", "type", null);
                //Get all rel types of this label
                List<RelationshipType> relTypes = relationshipTypeService.
                        findByLeftwardOrRightwardTypeName(context, key);
                //Get only possible Rel type of parent entity type, child entity type, and current label
                RelationshipType relationshipType = matchRelationshipType(relTypes,
                        childEntityTypeLabel, parentEntityTypeLabel, key);
                if (relationshipType == null) {
                    continue;
                }
                //Get all relationships of parent item and derived relationship type
                List<Relationship> relationships = relationshipService
                        .findByItemAndRelationshipType(context, parentItem, relationshipType);
                boolean alreadyExist = false;
                //Determine if rel already exist
                for (Relationship relationship : relationships) {
                    if (relationship.getRightItem() == childItem || relationship.getLeftItem() == childItem) {
                        alreadyExist = true;
                        break;
                    }
                }
                if (!alreadyExist) {
                    boolean isParentLeft = false;
                    Relationship persistedRelationship = null;
                    //Determine left or right rel side for parent
                    if (relationshipType.getLeftType().getLabel().equalsIgnoreCase(parentEntityTypeLabel)) {
                        isParentLeft = true;
                    }

                    int leftOrder = 0;
                    int rightOrder = 0;

                    Map<Integer, Integer> orders = relsMap.get(key).get(childUUIDString);
                    for (int left : orders.keySet()) {
                        leftOrder = left;
                        rightOrder = orders.get(left);
                    }
                    if (isParentLeft) {
                        persistedRelationship = relationshipService.create(context, parentItem, childItem,
                                relationshipType, leftOrder, rightOrder);
                    } else {
                        persistedRelationship = relationshipService.create(context, childItem, parentItem,
                                relationshipType, leftOrder, rightOrder);
                    }
                    relationshipService.update(context, persistedRelationship);
                }
            }
        }
    }

    public int getDSOTypeFromUUID(Context context, String uuid) throws SQLException {
        CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        if (itemService.find(context, UUID.fromString(uuid)) != null) {
            return Constants.ITEM;
        } else if (collectionService.find(context, UUID.fromString(uuid)) != null) {
            return Constants.COLLECTION;
        } else {
            return Constants.COMMUNITY;
        }
    }

    public String getMIMEType(PackageParameters params) {
        return (params != null &&
                (params.getBooleanProperty("manifestOnly", false))) ?
                "text/xml" : "application/zip";
    }

    public void dryRunDisseminate(Set<UUID> relatedUUIDSet) throws SQLException {
        for (UUID uuid : relatedUUIDSet) {
            System.out.println(uuid.toString());
        }
    }

    public void dryRunIngest(Context context, String sourceFileInit, String scope,
                             PackagerFileService packagerFileService) throws SQLException {
        List<PackagerFileService.FileNode> nodeTree = packagerFileService
                .getFileNodeTree(context, sourceFileInit, scope);
        if (nodeTree.size() > 0) {
            nodeTree.get(0).print(System.out);
        }
    }

    public static void dirAndFilePathBuilder(List<String> sourceFileIngest, String[] sourceFiles) {
        //ingest the object from the source file
        for (String sourceFile : sourceFiles) {
            File testFile = new File(sourceFile);
            if (testFile.exists() && testFile.isDirectory()) {
                for (String file : testFile.list()) {
                    sourceFileIngest.add(sourceFile + "/" + file);
                }
            } else {
                sourceFileIngest.add(sourceFile);
            }
        }
    }

    /**
     * Matches two Entity types to a Relationship Type from a set of Relationship Types.
     *
     * @param relTypes set of Relationship Types.
     * @param childEntityType entity type of target.
     * @param parentEntityType entity type of origin referer.
     * @return null or matched Relationship Type.
     */
    private RelationshipType matchRelationshipType(List<RelationshipType> relTypes,
                                                   String childEntityType, String parentEntityType,
                                                   String originTypeName) {
        RelationshipType foundRelationshipType = null;
        if (originTypeName.split("\\.").length > 1) {
            originTypeName = originTypeName.split("\\.")[1];
        }
        for (RelationshipType relationshipType : relTypes) {
            // Is origin type leftward or rightward
            boolean isLeft = false;
            if (relationshipType.getLeftType().getLabel().equalsIgnoreCase(parentEntityType)) {
                isLeft = true;
            }
            if (isLeft) {
                // Validate typeName reference
                if (!relationshipType.getLeftwardType().equalsIgnoreCase(originTypeName)) {
                    continue;
                }
                if (relationshipType.getLeftType().getLabel().equalsIgnoreCase(parentEntityType) &&
                        relationshipType.getRightType().getLabel().equalsIgnoreCase(childEntityType)) {
                    foundRelationshipType = relationshipType;
                }
            } else {
                if (!relationshipType.getRightwardType().equalsIgnoreCase(originTypeName)) {
                    continue;
                }
                if (relationshipType.getLeftType().getLabel().equalsIgnoreCase(childEntityType) &&
                        relationshipType.getRightType().getLabel().equalsIgnoreCase(parentEntityType)) {
                    foundRelationshipType = relationshipType;
                }
            }
        }
        return foundRelationshipType;
    }
}
