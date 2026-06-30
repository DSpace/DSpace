/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.factory.CrisLayoutServiceFactory;
import org.dspace.layout.script.service.CrisLayoutToolParser;
import org.dspace.layout.script.service.CrisLayoutToolValidationResult;
import org.dspace.layout.script.service.CrisLayoutToolValidator;
import org.dspace.layout.service.CrisLayoutTabService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Script to import CRIS layout configuration from excel file.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisLayoutToolScript extends DSpaceRunnable<CrisLayoutToolScriptConfiguration<CrisLayoutToolScript>> {

    private AuthorizeService authorizeService;

    private CrisLayoutTabService tabService;

    private CrisLayoutToolValidator validator;

    private CrisLayoutToolParser parser;

    private String filename;

    private Context context;

    @Override
    public void setup() throws ParseException {

        this.authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        this.validator = CrisLayoutServiceFactory.getInstance().getCrisLayoutToolValidator();
        this.parser = CrisLayoutServiceFactory.getInstance().getCrisLayoutToolParser();
        this.tabService = CrisLayoutServiceFactory.getInstance().getTabService();

        filename = commandLine.getOptionValue('f');
    }

    @Override
    public void internalRun() throws Exception {
        context = new Context(Context.Mode.BATCH_EDIT);
        assignCurrentUserInContext();
        assignSpecialGroupsInContext();

        context.turnOffAuthorisationSystem();

        if (!this.authorizeService.isAdmin(context)) {
            throw new IllegalArgumentException("The user cannot use the cris layout configuration tool");
        }

        InputStream inputStream = handler.getFileStream(context, filename)
            .orElseThrow(() -> new IllegalArgumentException("Error reading file, the file couldn't be "
                + "found for filename: " + filename));

        try {
            performImport(inputStream);
            context.complete();
            handler.logInfo("Import completed successfully");
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void performImport(InputStream inputStream) throws SQLException, AuthorizeException {
        Workbook workbook = createWorkbook(inputStream);

        validateWorkbook(workbook);
        handler.logInfo("The given workbook is valid. Proceed with the import");

        List<CrisLayoutTab> tabs = parser.parse(context, workbook);
        handler.logInfo("The workbook has been parsed correctly, found " + tabs.size() + " tabs to import");
        handler.logInfo("Proceed with the clearing of the previous layout");

        cleanUpLayout();
        handler.logInfo("The previous layout has been deleted, proceed with the import of the new configuration");

        tabs.forEach(this::importTab);
    }

    private Workbook createWorkbook(InputStream is) {
        try {
            return WorkbookFactory.create(is);
        } catch (EncryptedDocumentException | IOException e) {
            throw new IllegalArgumentException("An error occurs during the workbook creation", e);
        }
    }

    private void validateWorkbook(Workbook workbook) {
        CrisLayoutToolValidationResult validationResult = validator.validate(context, workbook);
        if (validationResult.isNotValid()) {
            validationResult.getErrors().forEach(handler::logError);
            throw new IllegalArgumentException("The given workbook is not valid. Import canceled");
        } else {
            validationResult.getWarnings().forEach(handler::logWarning);
        }
    }

    private void cleanUpLayout() throws SQLException, AuthorizeException {
        List<CrisLayoutTab> tabs = this.tabService.findAll(context);
        handler.logInfo("Found " + tabs.size() + " tabs to delete");
        for (CrisLayoutTab tab : tabs) {
            this.tabService.delete(context, tab);
        }

        context.flush();
    }

    private void importTab(CrisLayoutTab tab) {
        try {
            this.tabService.create(context, tab);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public CrisLayoutToolScriptConfiguration<CrisLayoutToolScript> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("cris-layout-tool",
            CrisLayoutToolScriptConfiguration.class);
    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void assignSpecialGroupsInContext() throws SQLException {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

}
