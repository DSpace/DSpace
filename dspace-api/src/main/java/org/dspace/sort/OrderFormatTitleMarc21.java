/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sort;

import org.dspace.text.filter.DecomposeDiactritics;
import org.dspace.text.filter.LowerCaseAndTrim;
import org.dspace.text.filter.MARC21InitialArticleWord;
import org.dspace.text.filter.StripLeadingNonAlphaNum;
import org.dspace.text.filter.TextFilter;

/**
 * MARC 21 title ordering delegate implementation
 *
 * @author Graham Triggs
 */
public class OrderFormatTitleMarc21 extends AbstractTextFilterOFD {
    {
        filters = new TextFilter[] {new MARC21InitialArticleWord(),
            new DecomposeDiactritics(),
            new StripLeadingNonAlphaNum(),
            new LowerCaseAndTrim()};
    }
}
