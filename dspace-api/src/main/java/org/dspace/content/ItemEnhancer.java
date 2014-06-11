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
