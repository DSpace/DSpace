/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/**
 * Feature for conveying a request that materials forbidden to the requester
 * by resource policy be made available by other means.  The request will be
 * e-mailed to a responsible party for consideration and action.  Find details
 * in the user documentation under the rubric "Request a Copy".
 *
 * <p>This package includes several "strategy" classes which discover responsible
 * parties in various ways.  See {@link RequestItemSubmitterStrategy} and the
 * classes which extend it.  A strategy class must be configured and identified
 * as {@link RequestItemAuthorExtractor} for injection into code which requires
 * Request a Copy services.
 */
package org.dspace.app.requestitem;
