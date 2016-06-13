/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkInternalException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;

/**
 * Wrap ReferCrosswalk to define behavioural header/footer   
 * 
 * @author pascarelli
 *
 */
public class HeaderFooterCitationCrosswalk extends ReferCrosswalk implements
        StreamGenericDisseminationCrosswalk
{

    @Override
    public void disseminate(Context context, List<DSpaceObject> dso,
            OutputStream out) throws CrosswalkException, IOException,
            SQLException, AuthorizeException
    {

        //write header
        String myName = getPluginInstanceName();
        if (myName == null)
            throw new CrosswalkInternalException(
                    "Cannot determine plugin name, "
                            + "You must use PluginManager to instantiate ReferCrosswalk so the instance knows its name.");

        String templatePropNameHeader = CONFIG_PREFIX + ".template." + myName + ".header";
        
        String templateFileNameHeader = ConfigurationManager
                .getProperty(templatePropNameHeader);

        if (templateFileNameHeader == null)
            throw new CrosswalkInternalException(
                    "Configuration error: "
                            + "No template header file configured for Refer crosswalk named \""
                            + myName + "\"");

        String parent = ConfigurationManager.getProperty("dspace.dir")
                + File.separator + "config" + File.separator;
        File templateFile = new File(parent, templateFileNameHeader);
        FileInputStream sourceHeader = new FileInputStream(templateFile);
        try {
        	Utils.bufferedCopy(sourceHeader, out);
        }
        finally {
        	sourceHeader.close();
        }


        for (DSpaceObject dsobject : dso)
        {
            super.disseminate(context, dsobject, out);
        }

        //write footer
        String templatePropNameFooter = CONFIG_PREFIX + ".template." + myName + ".footer";
        
        String templateFileNameFooter = ConfigurationManager
                .getProperty(templatePropNameFooter);

        if (templateFileNameFooter == null)
            throw new CrosswalkInternalException(
                    "Configuration error: "
                            + "No template footer file configured for Refer crosswalk named \""
                            + myName + "\"");

        String parentfooter = ConfigurationManager.getProperty("dspace.dir")
                + File.separator + "config" + File.separator;
        File templateFileFooter = new File(parentfooter, templateFileNameFooter);
        FileInputStream sourceFooter = new FileInputStream(templateFileFooter);
        try {
        	Utils.bufferedCopy(sourceFooter, out);
        }
        finally {
        	sourceFooter.close();
        }
    }
}