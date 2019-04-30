/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.configuration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.dto.SimpleViewEntityDTO;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public class CrisRPViewResolver implements ISimpleViewResolver
{

    /** log4j logger */
    private static Logger log = Logger.getLogger(CrisRPViewResolver.class);

    @Override
    public void fillDTO(Context context, SimpleViewEntityDTO dto, DSpaceObject dso)
    {
        if (!(dso instanceof ResearcherPage)) {
            return;
        }

        ResearcherPage rp = (ResearcherPage) dso;

        Integer epersonId = rp.getEpersonID();

        if (epersonId != null)
        {
            List<String> epersonInformations = new ArrayList<String>();
            try
            {
                EPerson eperson = EPerson.find(context, epersonId);
                if (eperson != null)
                {
                    String id = "" + eperson.getID();
                    String fullName = eperson.getFullName();
                    String email = eperson.getEmail();

                    epersonInformations.add(id);
                    epersonInformations.add(email);
                    epersonInformations.add(fullName);
                }
            }
            catch (SQLException e)
            {
                log.error(e.getMessage(), e);
            }
            dto.getDuplicateItem().put("eperson", epersonInformations);
        }
    }

}
