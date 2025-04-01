/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/**
 * Feature for conveying a request that materials forbidden to the requester
 * by resource policy be made available by other means.
 *
 * There are several methods of making the resource(s) available to the requester:
 * 1. The request will be e-mailed to a responsible party for consideration and action.
 * Find details in the user documentation under the rubric "Request a Copy".
 *
 * <p>Mailing is handled by {@link RequestItemEmailNotifier}.  Responsible
 * parties are represented by {@link RequestItemAuthor}
 *
 * 2. A unique 48-char token will be generated and included in a special weblink emailed to the requester.
 * This link will provide access to the requester as though they had READ policy access while the access period
 * has not expired, or forever if the access period is null.
 *
 * <p>This package includes several "strategy" classes which discover
 * responsible parties in various ways.  See
 * {@link RequestItemSubmitterStrategy} and the classes which extend it, and
 * others which implement {@link RequestItemAuthorExtractor}.  A strategy class
 * must be configured and identified as {@link requestItemAuthorExtractor}
 * (<em>note capitalization</em>) for injection into code which requires Request
 * a Copy services.
 */
package org.dspace.app.requestitem;
