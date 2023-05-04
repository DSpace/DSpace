/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.signposting.model.Linkset;
import org.dspace.app.rest.signposting.model.Lset;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * SignPostingProcessor interface.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public interface SignPostingProcessor<T extends DSpaceObject> {
    void buildRelation(Context context, HttpServletRequest request,
            T object, List<Linkset> linksets, Linkset primaryLinkset);

    void buildLset(Context context, HttpServletRequest request,
            T object, List<Lset> lsets);
}
