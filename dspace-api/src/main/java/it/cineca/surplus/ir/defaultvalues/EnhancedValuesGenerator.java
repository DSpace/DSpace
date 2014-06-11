package it.cineca.surplus.ir.defaultvalues;

import org.dspace.content.Item;

public interface EnhancedValuesGenerator
{
    public DefaultValuesBean generateValues(Item item, String schema, String element, String qualifier, String value);
}
