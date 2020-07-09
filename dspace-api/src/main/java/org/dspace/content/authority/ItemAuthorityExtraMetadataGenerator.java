package org.dspace.content.authority;

import java.util.List;
import java.util.Map;

import org.dspace.content.Item;

/**
 * 
 * Interface to manage simple/aggregation for extra values on authority
 * 
 * @author Mykhaylo Boychuk (4science.it)
 *
 */
public interface ItemAuthorityExtraMetadataGenerator {

    public Map<String, String> build(Item item);

    public List<Choice> buildAggregate(Item item);

}