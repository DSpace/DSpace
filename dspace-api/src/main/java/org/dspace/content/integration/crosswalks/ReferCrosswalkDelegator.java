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
 * Delegator class to call one of the {@link ReferCrosswalk} bean
 * searching it in the {@link ReferCrosswalkMapper}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ReferCrosswalkDelegator extends SelfNamedPlugin
    implements StreamDisseminationCrosswalk, FileNameDisseminator {

    public static String[] getPluginNames() {
        return getReferCrosswalkMapper().getTypes().toArray(new String[] {});
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {
        getReferCrosswalk().disseminate(context, dso, out);
    }

    @Override
    public String getFileName() {
        return getReferCrosswalk().getFileName();
    }

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return getReferCrosswalk().canDisseminate(context, dso);
    }

    @Override
    public String getMIMEType() {
        return getReferCrosswalk().getMIMEType();
    }

    private ReferCrosswalk getReferCrosswalk() {
        String instanceName = getPluginInstanceName();
        ReferCrosswalkMapper mapper = getReferCrosswalkMapper();
        ReferCrosswalk referCrosswalk = mapper.getReferCrosswalk(getPluginInstanceName());
        if (referCrosswalk == null) {
            throw new IllegalArgumentException("No ReferCrosswalk found for plugin instance name " + instanceName);
        }
        return referCrosswalk;
    }

    private static ReferCrosswalkMapper getReferCrosswalkMapper() {
        ReferCrosswalkMapper mapper = new DSpace().getSingletonService(ReferCrosswalkMapper.class);
        if (mapper == null) {
            throw new IllegalStateException("No ReferCrosswalkMapper defined in the Spring context");
        }
        return mapper;
    }

}