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

import org.apache.commons.lang3.StringUtils;

/**
 * This is a *very* stupid test fixture for authority control with AuthorityVariantsSupport.
 *
 * @author Andrea Bollini (CILEA)
 */
public class TestAuthority implements ChoiceAuthority, AuthorityVariantsSupport {
    private String pluginInstanceName;

    @Override
    public List<String> getVariants(String key, String locale) {
        if (StringUtils.isNotBlank(key)) {
            List<String> variants = new ArrayList<String>();
            for (int i = 0; i < 3; i++) {
                variants.add(key + "_variant#" + i);
            }
            return variants;
        }
        return null;
    }

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        Choices choices = new Choices(false);
        if (StringUtils.isNotBlank(text)) {

            List<Choice> choiceValues = new ArrayList<Choice>();
            for (int i = 0; i < 3; i++) {
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
    public Choices getBestMatch(String text, String locale) {
        Choices choices = new Choices(false);
        if (StringUtils.isNotBlank(text)) {

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
    public String getLabel(String key, String locale) {
        if (StringUtils.isNotBlank(key)) {
            return key.replaceAll("authority", "label");
        }
        return "Unknown";
    }

    @Override
    public String getPluginInstanceName() {
        return pluginInstanceName;
    }

    @Override
    public void setPluginInstanceName(String name) {
        this.pluginInstanceName = name;
    }
}
