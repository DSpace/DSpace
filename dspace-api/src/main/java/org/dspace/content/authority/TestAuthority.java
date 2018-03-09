/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Collection;

/**
 * This is a *very* stupid test fixture for authority control with AuthorityVariantsSupport.
 * 
 * @author Andrea Bollini (CILEA)
 */
public class TestAuthority implements ChoiceAuthority, AuthorityVariantsSupport
{

    @Override
    public List<String> getVariants(String key, String locale)
    {
        if (StringUtils.isNotBlank(key))
        {
            List<String> variants = new ArrayList<String>();
            for (int i = 0; i < 3; i++)
            {
                variants.add(key+"_variant#"+i);
            }
            return variants;
        }
        return null;
    }

    @Override
    public Choices getMatches(String field, String text, Collection collection,
            int start, int limit, String locale)
    {
        Choices choices = new Choices(false);
        if (StringUtils.isNotBlank(text))
        {
            
            List<Choice> choiceValues = new ArrayList<Choice>();
            for (int i = 0; i < 3; i++)
            {
                choiceValues.add(new Choice(text + "_authority#" + i, text
                        + "_value#" + i, text + "_label#" + i));
            }
            choices = new Choices(
                    (Choice[]) choiceValues.toArray(new Choice[choiceValues
                            .size()]), 0, 3, Choices.CF_AMBIGUOUS, false);
        }
        
        return choices;
    }

    @Override
    public Choices getBestMatch(String field, String text, Collection collection,
            String locale)
    {
        Choices choices = new Choices(false);
        if (StringUtils.isNotBlank(text))
        {
            
            List<Choice> choiceValues = new ArrayList<Choice>();
            
                choiceValues.add(new Choice(text + "_authoritybest", text
                        + "_valuebest", text + "_labelbest"));
            
            choices = new Choices(
                    (Choice[]) choiceValues.toArray(new Choice[choiceValues
                            .size()]), 0, 3, Choices.CF_UNCERTAIN, false);
        }
        return choices;
    }

    @Override
    public String getLabel(String field, String key, String locale)
    {
        if (StringUtils.isNotBlank(key))
        {
            return key.replaceAll("authority", "label");
        }
        return "Unknown";
    }
}
