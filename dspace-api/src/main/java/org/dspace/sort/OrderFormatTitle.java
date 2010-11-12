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
import org.dspace.text.filter.StandardInitialArticleWord;
import org.dspace.text.filter.TextFilter;
import org.dspace.sort.AbstractTextFilterOFD;

/**
 * Standard title ordering delegate implementation
 * 
 * @author Graham Triggs
 */
public class OrderFormatTitle extends AbstractTextFilterOFD
{
	{
		filters = new TextFilter[] { new StandardInitialArticleWord(),
									 new DecomposeDiactritics(),
									 new LowerCaseAndTrim() };
	}
}
