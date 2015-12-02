/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.io.File;
import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.dspace.app.sfx.factory.SfxServiceFactory;
import org.dspace.app.sfx.service.SFXFileReaderService;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;


/**
 * Renders an SFX query link. Takes one attribute - "item" which must be an Item
 * object.
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class SFXLinkTag extends TagSupport
{
    /** Item to display SFX link for */

    private Item item;

    /** The fully qualified pathname of the SFX  XML file */
    private final String sfxFile = ConfigurationManager.getProperty("dspace.dir") + File.separator
    				+ "config" + File.separator + "sfx.xml";

    private static final long serialVersionUID = 7028793612957710128L;

    private final transient SFXFileReaderService sfxFileReaderService
            = SfxServiceFactory.getInstance().getSfxFileReaderService();
    
    public SFXLinkTag()
    {
        super();
    }

    @Override
    public int doStartTag() throws JspException
    {
        try
        {
            String sfxServer = ConfigurationManager
                    .getProperty("sfx.server.url");

            if (sfxServer == null)
            {
                // No SFX server - SFX linking switched off
                return SKIP_BODY;
            }

	    String sfxQuery = "";

            sfxQuery = sfxFileReaderService.loadSFXFile(sfxFile, item);

            // Remove initial &, if any
            if (sfxQuery.startsWith("&"))
            {
                sfxQuery = sfxQuery.substring(1);
            }
            pageContext.getOut().print(sfxServer + sfxQuery);
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }

        return SKIP_BODY;
    }

    /**
     * Get the item this tag should display SFX Link for
     *
     * @return the item
     */
    public Item getItem()
    {
        return item;
    }

    /**
     * Set the item this tag should display SFX Link for
     *
     * @param itemIn
     *            the item
     */
    public void setItem(Item itemIn)
    {
        item = itemIn;
    }

}
