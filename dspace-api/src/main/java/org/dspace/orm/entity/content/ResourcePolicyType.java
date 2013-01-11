/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity.content;

/**
 * This class is intended to replace the use of constants
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
public enum ResourcePolicyType {
	TYPE_SUBMISSION,
	TYPE_WORKFLOW,
	TYPE_CUSTOM,
	TYPE_INHERITED
}
