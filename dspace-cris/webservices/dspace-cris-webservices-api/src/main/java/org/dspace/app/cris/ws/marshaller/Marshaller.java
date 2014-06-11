/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws.marshaller;

import java.util.List;
import org.jdom.Element;

public interface Marshaller<T>
{
    public Element buildResponse(List<T> results, long start, long tot, String type,
            String[] splitProjection, boolean showHiddenMetadata, String nameRoot);
}
