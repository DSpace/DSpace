/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.importer.external.MetadataSourceException;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.ImportService;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 01/10/15
 * Time: 15:33
 */
public class LiveImportStep extends AbstractProcessingStep {
    private String url = ConfigurationManager.getProperty("elsevier-sciencedirect", "api.scidir.url");
    private static Logger log = Logger.getLogger(LiveImportStep.class);

    protected static ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Override
    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        String importId = request.getParameter("import_id");

        if(StringUtils.isBlank(importId)){
            String buttonPressed = Util.getSubmitButton(request, "");

            if(buttonPressed.startsWith("submit-import-")){
                importId = buttonPressed.substring("record-import-".length());
            }
        }

        if (StringUtils.isNotBlank(importId)) {
            ImportService importService = new DSpace().getServiceManager().getServiceByName(null, ImportService.class);
            Item item = subInfo.getSubmissionItem().getItem();
            try {
                ImportRecord record = importService.getRecord(url, "eid(" + importId + ")");

                itemService.clearMetadata(context,item,Item.ANY,Item.ANY,Item.ANY,Item.ANY);

                for (MetadatumDTO metadatum : record.getValueList()) {
                    itemService.addMetadata(context, item, metadatum.getSchema(), metadatum.getElement(), metadatum.getQualifier(), metadatum.getLanguage(), metadatum.getValue());
                }

                itemService.update(context,item);
                context.dispatchEvents();
            } catch (MetadataSourceException e) {
                log.error(e.getMessage(), e);
            }
        }
        return 0;
    }

    @Override
    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) throws ServletException {
        return 1;
    }
}
