/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.itemimport.service.ItemImportService;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * CLI variant for the {@link ItemImport} class.
 * This was done to specify the specific behaviors for the CLI.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemImportCLI extends ItemImport {

    @Override
    protected void validate(Context context) {
        // can only resume for adds
        if (isResume && !"add".equals(command)) {
            handler.logError("Resume option only works with the --add command (run with -h flag for details)");
            throw new UnsupportedOperationException("Resume option only works with the --add command");
        }

        if (commandLine.hasOption('e')) {
            eperson = commandLine.getOptionValue('e');
        }

        // check eperson identifier (email or id)
        if (eperson == null) {
            handler.logError("An eperson to do the importing must be specified (run with -h flag for details)");
            throw new UnsupportedOperationException("An eperson to do the importing must be specified");
        }

        File myFile = null;
        try {
            myFile = new File(mapfile);
        } catch (Exception e) {
            throw new UnsupportedOperationException("The mapfile " + mapfile + " does not exist");
        }

        if (!isResume && "add".equals(command) && myFile.exists()) {
            handler.logError("The mapfile " + mapfile + " already exists. "
                    + "Either delete it or use --resume if attempting to resume an aborted import. "
                    + "(run with -h flag for details)");
            throw new UnsupportedOperationException("The mapfile " + mapfile + " already exists");
        }

        if (command == null) {
            handler.logError("Must run with either add, replace, or remove (run with -h flag for details)");
            throw new UnsupportedOperationException("Must run with either add, replace, or remove");
        } else if ("add".equals(command) || "replace".equals(command)) {
            if (!remoteUrl && sourcedir == null) {
                handler.logError("A source directory containing items must be set (run with -h flag for details)");
                throw new UnsupportedOperationException("A source directory containing items must be set");
            }

            if (mapfile == null) {
                handler.logError(
                        "A map file to hold importing results must be specified (run with -h flag for details)");
                throw new UnsupportedOperationException("A map file to hold importing results must be specified");
            }
        } else if ("delete".equals(command)) {
            if (mapfile == null) {
                handler.logError("A map file must be specified (run with -h flag for details)");
                throw new UnsupportedOperationException("A map file must be specified");
            }
        }
    }

    @Override
    protected void process(Context context, ItemImportService itemImportService,
            List<Collection> collections) throws Exception {
        if ("add".equals(command)) {
            itemImportService.addItems(context, collections, sourcedir, mapfile, template);
        } else if ("replace".equals(command)) {
            itemImportService.replaceItems(context, collections, sourcedir, mapfile, template);
        } else if ("delete".equals(command)) {
            itemImportService.deleteItems(context, mapfile);
        }
    }

    @Override
    protected void readZip(Context context, ItemImportService itemImportService) throws Exception {
        // If this is a zip archive, unzip it first
        if (zip) {
            if (!remoteUrl) {
                // confirm zip file exists
                File myZipFile = new File(sourcedir + File.separator + zipfilename);
                if ((!myZipFile.exists()) || (!myZipFile.isFile())) {
                    throw new IllegalArgumentException(
                        "Error reading file, the file couldn't be found for filename: " + zipfilename);
                }

                // validate zip file
                InputStream validationFileStream = new FileInputStream(myZipFile);
                validateZip(validationFileStream);

                workDir = new File(itemImportService.getTempWorkDir() + File.separator + TEMP_DIR
                        + File.separator + context.getCurrentUser().getID());
                sourcedir = itemImportService.unzip(
                        new File(sourcedir + File.separator + zipfilename), workDir.getAbsolutePath());
            } else {
                // manage zip via remote url
                Optional<InputStream> optionalFileStream = Optional.ofNullable(new URL(zipfilename).openStream());
                if (optionalFileStream.isPresent()) {
                    // validate zip file via url
                    Optional<InputStream> validationFileStream = Optional.ofNullable(new URL(zipfilename).openStream());
                    if (validationFileStream.isPresent()) {
                        validateZip(validationFileStream.get());
                    }

                    workFile = new File(itemImportService.getTempWorkDir() + File.separator
                            + zipfilename + "-" + context.getCurrentUser().getID());
                    FileUtils.copyInputStreamToFile(optionalFileStream.get(), workFile);
                    workDir = new File(itemImportService.getTempWorkDir() + File.separator + TEMP_DIR
                                       + File.separator + context.getCurrentUser().getID());
                    sourcedir = itemImportService.unzip(workFile, workDir.getAbsolutePath());
                } else {
                    throw new IllegalArgumentException(
                            "Error reading file, the file couldn't be found for filename: " + zipfilename);
                }
            }
        }
    }

    @Override
    protected void setMapFile() {
        if (commandLine.hasOption('m')) {
            mapfile = commandLine.getOptionValue('m');
        }
    }

    @Override
    protected void setZip() {
        if (commandLine.hasOption('s')) { // source
            sourcedir = commandLine.getOptionValue('s');
        }

        if (commandLine.hasOption('z')) {
            zip = true;
            zipfilename = commandLine.getOptionValue('z');
        }

        if (commandLine.hasOption('u')) { // remote url
            zip = true;
            remoteUrl = true;
            zipfilename = commandLine.getOptionValue('u');
        }
    }

    @Override
    protected void setEPerson(Context context) throws SQLException {
        EPerson myEPerson = null;
        if (StringUtils.contains(eperson, '@')) {
            // @ sign, must be an email
            myEPerson = epersonService.findByEmail(context, eperson);
        } else {
            myEPerson = epersonService.find(context, UUID.fromString(eperson));
        }

        // check eperson
        if (myEPerson == null) {
            handler.logError("EPerson cannot be found: " + eperson + " (run with -h flag for details)");
            throw new UnsupportedOperationException("EPerson cannot be found: " + eperson);
        }

        context.setCurrentUser(myEPerson);
    }
}
