/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.migration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Script (config {@link SubmissionFormsMigrationCliScriptConfiguration}) to transform the old input-forms.xml and
 * item-submission.xml via XSLT (xsl files in dspace/config/migration) into respectively the new submission-forms.xsl
 * and item-submissions.xsl files
 *
 * @author Maria Verdonck (Atmire) on 13/11/2020
 */
public class SubmissionFormsMigration extends DSpaceRunnable<SubmissionFormsMigrationCliScriptConfiguration> {

    private boolean help = false;
    private String inputFormsFilePath = null;
    private String itemSubmissionsFilePath = null;

    private static final String PATH_OUT_CONFIG =
        DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir")
        + File.separator + "config";

    private static final String PATH_XSL_SUBMISSION_FORMS =
        PATH_OUT_CONFIG + File.separator + "migration" + File.separator + "submission-forms.xsl";
    private static final String PATH_XSL_ITEM_SUBMISSION =
        PATH_OUT_CONFIG + File.separator + "migration" + File.separator + "item-submissions.xsl";

    private static final String PATH_OUT_INPUT_FORMS =
        PATH_OUT_CONFIG + File.separator + "submission-forms.xml.migrated";
    private static final String PATH_OUT_ITEM_SUBMISSION =
        PATH_OUT_CONFIG + File.separator + "item-submission.xml.migrated";

    private static final String NAME_DTD_INPUT_FORMS = "input-forms.dtd";
    private static final String NAME_DTD_ITEM_SUBMISSION = "item-submission.dtd";
    private static final String CONTENT_DTD_ITEM_SUBMISSION_DUMMY =
        "<!ELEMENT item-submission (submission-map, step-definitions, submission-definitions) >";
    private static final String CONTENT_DTD_INPUT_FORMS_DUMMY =
        "<!ELEMENT input-forms (form-map, form-definitions, form-value-pairs) >";
    private List<File> tempFiles = new ArrayList<>();

    @Override
    public void internalRun() throws TransformerException {
        if (help) {
            printHelp();
            return;
        }
        if (this.inputFormsFilePath != null) {
            this.transform(inputFormsFilePath, PATH_XSL_SUBMISSION_FORMS, PATH_OUT_INPUT_FORMS);
        }
        if (this.itemSubmissionsFilePath != null) {
            this.transform(itemSubmissionsFilePath, PATH_XSL_ITEM_SUBMISSION, PATH_OUT_ITEM_SUBMISSION);
        }
        deleteTempFiles();
    }

    /**
     * Transforms an input xml file to an output xml file with the given xsl file
     *
     * @param sourceFilePath Input XML
     * @param xsltFilePath   Transforming XSL
     * @param outputPath     Output XML
     */
    private void transform(String sourceFilePath, String xsltFilePath, String outputPath)
        throws TransformerException {
        handler.logInfo("Transforming " + sourceFilePath + " with xsl: " + xsltFilePath + " to output: " + outputPath);

        Source xmlSource = new StreamSource(new File(sourceFilePath));
        Source xsltSource = new StreamSource(new File(xsltFilePath));
        Result result = new StreamResult(new File(outputPath));

        // Create an instance of TransformerFactory
        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        Transformer trans;
        try {
            trans = transformerFactory.newTransformer(xsltSource);
            trans.setParameter("inputFormsPath", inputFormsFilePath);
        } catch (TransformerConfigurationException e) {
            handler.logError("Error: the stylesheet at '" + xsltFilePath + "' couldn't be used");
            throw e;
        }

        try {
            trans.transform(xmlSource, result);
        } catch (Throwable t) {
            handler.logError("Error: couldn't convert the metadata file at '" + sourceFilePath);
            throw t;
        }
    }

    @Override
    public void setup() throws ParseException {
        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }
        if (commandLine.hasOption('f')) {
            inputFormsFilePath = commandLine.getOptionValue('f');
            checkIfValidXMLFile(inputFormsFilePath);
            inputFormsFilePath = getAbsolutePath(inputFormsFilePath);
        }
        if (commandLine.hasOption('s')) {
            itemSubmissionsFilePath = commandLine.getOptionValue('s');
            checkIfValidXMLFile(itemSubmissionsFilePath);
            itemSubmissionsFilePath = getAbsolutePath(itemSubmissionsFilePath);
        }
        if (!commandLine.hasOption('s') || !commandLine.hasOption('f')) {
            this.throwParseException("Please fill in both -f <source-input-forms-path> and -s " +
                                     "<source-item-submissions-path>");
        }
        createDTDFileDummiesIfNotPresent();
    }

    private String getAbsolutePath(String relativePath) {
        File file = new File(relativePath);
        return file.getAbsolutePath();
    }

    private void createDTDFileDummiesIfNotPresent() {
        // Create temporary dummy item-submission.dtd in directory of input item-submission.xml if not present
        this.createDummyFileIfNotPresent(itemSubmissionsFilePath, NAME_DTD_ITEM_SUBMISSION,
                                         CONTENT_DTD_ITEM_SUBMISSION_DUMMY);
        // Create temporary dummy input-forms.dtd in directory of input input-forms.xml if not present
        this.createDummyFileIfNotPresent(inputFormsFilePath, NAME_DTD_INPUT_FORMS, CONTENT_DTD_INPUT_FORMS_DUMMY);
    }

    private void createDummyFileIfNotPresent(String fileInInputDir, String dummyFileName, String dummyContent) {
        String dir = StringUtils.substringBeforeLast(fileInInputDir, File.separator);
        File dummyFile = new File (dir + File.separator + dummyFileName);
        if (!dummyFile.isFile()) {
            Path path = Paths.get(dir + File.separator + dummyFileName);
            byte[] strToBytes = dummyContent.getBytes();

            try {
                Files.write(path, strToBytes);
            } catch (IOException e) {
                handler.logError("Error trying to create dummy " + dummyFileName);
            }
            tempFiles.add(dummyFile);
        }
    }

    private void deleteTempFiles() {
        for (File tempFile: tempFiles) {
            if (tempFile != null && tempFile.isFile()) {
                tempFile.delete();
            }
        }
    }

    private void checkIfValidXMLFile(String filePath) throws ParseException {
        File file = new File(filePath);
        if (!file.exists()) {
            this.throwParseException("There is no file at path: " + filePath);
        }
        if (!file.isFile() && file.isDirectory()) {
            this.throwParseException("This is a dir, not a file: " + filePath);
        }
        if (!file.getName().endsWith(".xml")) {
            this.throwParseException("This is not an XML file (doesn't end in .xml): " + filePath);
        }
    }

    private void throwParseException(String message) throws ParseException {
        handler.logError(message);
        throw new ParseException(message);
    }

    @Override
    public SubmissionFormsMigrationCliScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("submission-forms-migrate",
            SubmissionFormsMigrationCliScriptConfiguration.class);
    }
}
