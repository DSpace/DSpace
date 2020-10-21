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
import java.util.Iterator;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.utils.DSpace;

/**
 * Delegator class to call one of the {@link StreamDisseminationCrosswalk} bean
 * searching it in the {@link StreamDisseminationCrosswalkMapper}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class StreamDisseminationCrosswalkDelegator extends SelfNamedPlugin
    implements StreamDisseminationCrosswalk, FileNameDisseminator {

    public static String[] getPluginNames() {
        return getStreamDisseminationCrosswalkMapper().getTypes().toArray(new String[] {});
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {
        getStreamDisseminationCrosswalk().disseminate(context, dso, out);
    }

    @Override
    public void disseminate(Context context, Iterator<? extends DSpaceObject> dsoIterator, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {
        getStreamDisseminationCrosswalk().disseminate(context, dsoIterator, out);
    }

    @Override
    public String getFileName() {
        StreamDisseminationCrosswalk crosswalk = getStreamDisseminationCrosswalk();
        if (crosswalk instanceof FileNameDisseminator) {
            return ((FileNameDisseminator) crosswalk).getFileName();
        } else {
            return null;
        }
    }

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return getStreamDisseminationCrosswalk().canDisseminate(context, dso);
    }

    @Override
    public String getMIMEType() {
        return getStreamDisseminationCrosswalk().getMIMEType();
    }

    private StreamDisseminationCrosswalk getStreamDisseminationCrosswalk() {
        String instanceName = getPluginInstanceName();
        StreamDisseminationCrosswalkMapper mapper = getStreamDisseminationCrosswalkMapper();
        StreamDisseminationCrosswalk streamDisseminationCrosswalk = mapper.getByType(getPluginInstanceName());
        if (streamDisseminationCrosswalk == null) {
            throw new IllegalArgumentException("No crosswalk found for plugin instance name " + instanceName);
        }
        return streamDisseminationCrosswalk;
    }

    private static StreamDisseminationCrosswalkMapper getStreamDisseminationCrosswalkMapper() {
        StreamDisseminationCrosswalkMapper mapper = new DSpace()
            .getSingletonService(StreamDisseminationCrosswalkMapper.class);
        if (mapper == null) {
            throw new IllegalStateException("No StreamDisseminationCrosswalkMapper defined in the Spring context");
        }
        return mapper;
    }

}