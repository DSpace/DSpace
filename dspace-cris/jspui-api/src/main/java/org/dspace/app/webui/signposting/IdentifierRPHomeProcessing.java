/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.signposting;

import java.beans.PropertyEditor;
import java.text.MessageFormat;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ICrisHomeProcessor;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.plugin.PluginException;

/**
 * @author Pascarelli Luigi Andrea
 */
public class IdentifierRPHomeProcessing
        implements ICrisHomeProcessor<ResearcherPage>
{

    /** log4j category */
    private static Logger log = Logger
            .getLogger(IdentifierRPHomeProcessing.class);
    
    private String metadataField;

    private ApplicationService applicationService;

    private String relationHeader;

    private String pattern;

    @Override
    public Class<ResearcherPage> getClazz()
    {
        return ResearcherPage.class;
    }

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response, ResearcherPage entity)
            throws PluginException, AuthorizeException
    {
        try
        {
            List<RPProperty> values = entity.getAnagrafica4view()
                    .get(getMetadataField());
            for (RPProperty val : values)
            {
                PropertyEditor pe = val.getTypo().getRendering()
                        .getPropertyEditor(applicationService);
                pe.setValue(val.getObject());
                response.addHeader("Link",
                        MessageFormat.format(pattern, pe.getAsText())
                                + "; rel=\"" + getRelationHeader() + "\"");
            }
        }
        catch (Exception ex)
        {
            log.error("Problem to add signposting pattern", ex);
        }
    }

    public String getMetadataField()
    {
        return metadataField;
    }

    public void setMetadataField(String metadataField)
    {
        this.metadataField = metadataField;
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public String getRelationHeader()
    {
        return relationHeader;
    }

    public void setRelationHeader(String relationHeader)
    {
        this.relationHeader = relationHeader;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

}
