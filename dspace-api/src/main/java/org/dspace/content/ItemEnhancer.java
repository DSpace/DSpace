/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import it.cineca.surplus.ir.defaultvalues.EnhancedValuesGenerator;

import java.util.List;

public class ItemEnhancer extends AItemEnhancer
{

    private List<EnhancedValuesGenerator> generators;

    public List<EnhancedValuesGenerator> getGenerators()
    {
        return generators;
    }

    public void setGenerators(List<EnhancedValuesGenerator> generators)
    {
        this.generators = generators;
    }
    
}
