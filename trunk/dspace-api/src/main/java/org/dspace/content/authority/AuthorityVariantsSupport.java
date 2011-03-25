/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.content.authority;

import java.util.List;

/**
 *
 * @author bollini
 */
public interface AuthorityVariantsSupport {
    public List<String> getVariants(String key, String locale);
}
