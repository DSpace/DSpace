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

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.utils.DSpace;

/**
 * Delegator class to call one of the {@link DocumentCrosswalk} bean
 * searching it in the {@link DocumentCrosswalkMapper}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DocumentCrosswalkDelegator extends SelfNamedPlugin
    implements StreamDisseminationCrosswalk, FileNameDisseminator {

    public static String[] getPluginNames() {
        return getDocumentCrosswalkMapper().getTypes().toArray(new String[] {});
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {
        getDocumentCrosswalk().disseminate(context, dso, out);
    }

    @Override
    public String getFileName() {
        return getDocumentCrosswalk().getFileName();
    }

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return getDocumentCrosswalk().canDisseminate(context, dso);
    }

    @Override
    public String getMIMEType() {
        return getDocumentCrosswalk().getMIMEType();
    }

    private DocumentCrosswalk getDocumentCrosswalk() {
        String instanceName = getPluginInstanceName();
        DocumentCrosswalkMapper mapper = getDocumentCrosswalkMapper();
        DocumentCrosswalk pdfDisseminationCrosswalk = mapper.getDisseminationCrosswalk(getPluginInstanceName());
        if (pdfDisseminationCrosswalk == null) {
            throw new IllegalArgumentException("No DocumentCrosswalk found for plugin instance name " + instanceName);
        }
        return pdfDisseminationCrosswalk;
    }

    private static DocumentCrosswalkMapper getDocumentCrosswalkMapper() {
        DocumentCrosswalkMapper mapper = new DSpace().getSingletonService(DocumentCrosswalkMapper.class);
        if (mapper == null) {
            throw new IllegalStateException("No DocumentCrosswalkMapper defined in the Spring context");
        }
        return mapper;
    }

}
