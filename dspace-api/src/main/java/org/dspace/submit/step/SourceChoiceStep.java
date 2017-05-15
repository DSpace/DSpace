/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.commons.lang.*;
import org.dspace.app.util.*;
import org.dspace.authorize.*;
import org.dspace.content.*;
import org.dspace.content.factory.*;
import org.dspace.content.service.*;
import org.dspace.core.*;
import org.dspace.importer.external.service.*;
import org.dspace.submit.*;
import org.dspace.utils.*;

/**
 * @author lotte.hofstede at atmire.com
 */
public class SourceChoiceStep extends AbstractProcessingStep {
    private Map<String, AbstractImportMetadataSourceService> sources = new DSpace().getServiceManager().getServiceByName("ImportServices", HashMap.class);
    public static final String CONDITIONAL_NEXT_IMPORT = "submit_condition_next_import";
    protected static ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        String source = request.getParameter("source");
        Item item = subInfo.getSubmissionItem().getItem();
        itemService.clearMetadata(context, item, "workflow", "import", "source", Item.ANY);
        if (StringUtils.isNotBlank(source) && sources.keySet().contains(source)) {
            itemService.addMetadata(context, item, "workflow", "import", "source", null, source);
            itemService.update(context, item);
            context.commit();
        }
        return 0;
    }

    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) throws ServletException {
        return 1;
    }
}
