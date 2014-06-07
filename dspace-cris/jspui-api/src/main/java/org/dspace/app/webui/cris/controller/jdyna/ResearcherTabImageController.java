/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.jdyna;

import it.cilea.osd.jdyna.controller.AFormTabController;
import it.cilea.osd.jdyna.web.Tab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class ResearcherTabImageController implements Controller
{
    /** log4j category */
    private static Logger log = Logger
            .getLogger(ResearcherTabImageController.class);

    /**
     * the applicationService for query the RP db, injected by Spring IoC
     */
    private ApplicationService applicationService;

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest req,
            HttpServletResponse response) throws Exception
    {

        String idString = req.getPathInfo();
        String[] pathInfo = idString.split("/", 3);
        String tabId = pathInfo[2];

        
        String contentType = "";
        String ext = "";

        Tab tab = applicationService.get(Tab.class, Integer.parseInt(tabId));
        ext = tab.getExt();
        contentType = tab.getMime();
        if (ext != null && !ext.isEmpty() && contentType != null
                && !contentType.isEmpty())
        {
            File image = new File(
                    tab.getFileSystemPath()
                            + File.separatorChar
                            + AFormTabController.DIRECTORY_TAB_ICON
                            + File.separatorChar
                            + AFormTabController.PREFIX_TAB_ICON
                            + tabId
                            + "."
                            + ext);

            InputStream is = null;
            try
            {
                is = new FileInputStream(image);

                response.setContentType(contentType);

                // Response length
                response.setHeader("Content-Length",
                        String.valueOf(image.length()));

                Utils.bufferedCopy(is, response.getOutputStream());
                is.close();
                response.getOutputStream().flush();
            }
            catch (FileNotFoundException not)
            {
                log.error(not.getMessage());
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            finally
            {
                if (is != null)
                {
                    is.close();
                }
            }
        }

        return null;
    }
}
