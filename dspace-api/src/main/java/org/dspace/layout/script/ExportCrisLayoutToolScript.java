/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.layout.factory.CrisLayoutServiceFactory;
import org.dspace.layout.script.service.CrisLayoutToolConverter;
import org.dspace.layout.service.CrisLayoutTabService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Script to export CRIS layout configuration into excel file.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 *
 */
public class ExportCrisLayoutToolScript
    extends DSpaceRunnable<ExportCrisLayoutToolScriptConfiguration<ExportCrisLayoutToolScript>> {

    private AuthorizeService authorizeService;

    private CrisLayoutTabService tabService;

    private CrisLayoutToolConverter converter;

    private Context context;

    @Override
    public void setup() throws ParseException {
        this.authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        this.converter = CrisLayoutServiceFactory.getInstance().getCrisLayoutToolConverter();
        this.tabService = CrisLayoutServiceFactory.getInstance().getTabService();
    }

    @Override
    public void internalRun() throws Exception {

        context = new Context();
        assignCurrentUserInContext();
        assignSpecialGroupsInContext();

        context.turnOffAuthorisationSystem();

        if (!this.authorizeService.isAdmin(context)) {
            throw new IllegalArgumentException("The user cannot use the cris layout configuration tool");
        }

        try {
            performExport();
            context.complete();
            handler.logInfo("Export has completed successfully");
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void performExport() throws Exception {
        Workbook workbook = converter.convert(tabService.findAll(context));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        handler.writeFilestream(context, getFileName(), in, getMIMEType());

        handler.logInfo("Layout exported successfully into file named " + getFileName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public ExportCrisLayoutToolScriptConfiguration<ExportCrisLayoutToolScript> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("export-cris-layout-tool",
            ExportCrisLayoutToolScriptConfiguration.class);
    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void assignSpecialGroupsInContext() {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

    private String getFileName() {
        return "cris-layout-tool-exported.xls";
    }

    public String getMIMEType() {
        return "application/vnd.ms-excel";
    }
}
