/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.servlet;

import it.cilea.osd.jdyna.web.tag.JDynATagLibraryFunctions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.core.ConfigurationManager;
import org.springframework.util.FileCopyUtils;

public class ResearcherPictureServlet extends HttpServlet
{
    /** log4j category */
    private static Logger log = Logger
            .getLogger(ResearcherPictureServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response)
            throws ServletException, IOException
    {

        String idString = req.getPathInfo();
        String[] pathInfo = idString.split("/", 2);
        String authorityKey = pathInfo[1];

        Integer id = ResearcherPageUtils.getRealPersistentIdentifier(
                authorityKey, ResearcherPage.class);

        if (id == null)
        {

            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        ResearcherPage rp = ResearcherPageUtils.getCrisObject(id,
                ResearcherPage.class);
        File file = new File(
                ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "researcherpage.file.path")                        
                        + File.separatorChar
                        + JDynATagLibraryFunctions.getFileFolder(rp.getPict()
                                .getValue())
                        + File.separatorChar
                        + JDynATagLibraryFunctions.getFileName(rp.getPict()
                                .getValue()));

        InputStream is = null;
        try
        {
            if (file.exists() && !file.isDirectory())
            {
                is = new FileInputStream(file);

            }
            else
            {
                String imgRedirected = "";
                Object defaultPlatformUserImg = req.getAttribute("anonymousUserImg");
                if(defaultPlatformUserImg==null) {
                    imgRedirected = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "researcherpage.image-default.path");
                }
                else {
                    imgRedirected = String.valueOf(defaultPlatformUserImg);
                }
                response.sendRedirect(imgRedirected);
                return;
            }
            response.setContentType(req.getSession().getServletContext()
                    .getMimeType(file.getName()));
            Long len = file.length();
            response.setContentLength(len.intValue());
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + file.getName());
            FileCopyUtils.copy(is, response.getOutputStream());
            response.getOutputStream().flush();
        }
        finally
        {
            if (is != null)
            {
                IOUtils.closeQuietly(is);
            }

        }

    }
}
