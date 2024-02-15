/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

/**
 * Projection used as a discriminator in ItemConverter to decide whether to trigger
 * retrieving the owning collection of a given Item.
 * This classes serves three needs:
 * (1) Retrieve the owning collection only when an Item is converted in the context
 *     of a Filtered Items report execution;
 * (2) Avoid this extra workload in any other context;
 * (3) Prevent embedding the owning collection in any other context, thus avoiding
 *     failing integration tests in ItemRestRepositoryIT.
 *
 * This projection behaves like DefaultProjection: it does no transformation, and
 * allows linking but not embedding of all subresources.
 */
public class ContentReportOwningCollectionProjection extends AbstractProjection {

    public final static String NAME = "contentreportowningcollection";

    @Override
    public String getName() {
        return NAME;
    }
}
