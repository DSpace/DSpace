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
