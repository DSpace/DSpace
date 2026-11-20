/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import jakarta.annotation.PostConstruct;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.METSDisseminationCrosswalk;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.Context;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Implementation of {@link StreamDisseminationCrosswalk} that produces a METS
 * manifest for the DSpace item as a metadata description, using
 * {@link METSDisseminationCrosswalk}.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class METSStreamDisseminationCrosswalk implements StreamDisseminationCrosswalk {

    private METSDisseminationCrosswalk metsDisseminationCrosswalk;

    @PostConstruct
    public void setup() {
        metsDisseminationCrosswalk = new METSDisseminationCrosswalk("AIP");
    }

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return metsDisseminationCrosswalk.canDisseminate(dso);
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        Element element = metsDisseminationCrosswalk.disseminateElement(context, dso);

        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        xmlOutputter.output(element, out);

    }

    @Override
    public String getMIMEType() {
        return "application/xml";
    }

}
