/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/**
 * Adapt Saxon-HE to DSpace's requirements.
 *
 * <p>
 * DSpace needs to define some custom XPath functions in Java code.  This requires
 * configuring Saxon Transformer factories, which would be simple using an
 * external configuration file in Saxon-PE, but this feature was not implemented
 * in Saxon-HE.  So we wrap the Saxon class in a configurable
 * extension:  {@link ConfigurableSaxonTransformerFactory}.
 *
 * <p>
 * The custom function implementations are in e.g. {@link ConfmanGetProperty}
 * and {@link ConfmanGetIntProperty2}.
 */

package org.dspace.saxon;
