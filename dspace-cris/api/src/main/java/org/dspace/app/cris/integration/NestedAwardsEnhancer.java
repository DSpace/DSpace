/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.model.TypeSupport;
import it.cilea.osd.jdyna.value.TextValue;
import it.cilea.osd.jdyna.widget.WidgetTesto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.VisibilityConstants;

public class NestedAwardsEnhancer extends CrisEnhancer
{
    private Log log = LogFactory.getLog(getClass());

    @Override
    protected <P extends Property<TP>, TP extends PropertiesDefinition> List<P> calculateProperties(
            AnagraficaSupport<P, TP> anagraficaSupport, String path)
    {

        List<P> results = new ArrayList<P>();

        Set<String> temporaryAwardsWith = new TreeSet<String>();
        TypeSupport<P, TP> cris = (TypeSupport<P, TP>) anagraficaSupport;

        if (cris.getTypo().getShortName().equals("awards"))
        {
            StringBuffer sb = new StringBuffer();

            boolean create = false;

            for (P subprop : cris.getAnagrafica4view().get(
                    "awardswith"))
            {
                String ssss = subprop.toString();
                Pattern p = Pattern.compile(".*rp(.*?)");
                Matcher m = p.matcher(ssss);

                String authority = "";

                if (m.matches())
                {
                    authority = m.group(1);
                }

                String result = "";
                String[] split = ssss.split("\\|\\|\\|");
                String replace = split[0];
                result += replace.toLowerCase();
                result += "|||" + replace;
                if (authority != null && !authority.isEmpty())
                {
                    result += "|||rp" + authority;
                }
                if (!result.isEmpty())
                {
                    temporaryAwardsWith.add(result);
                    create = true;
                }
            }

            if (create)
            {
                for (P subprop : cris.getAnagrafica4view().get(
                        "awardsdate"))
                {

                    sb.append("###").append("awardsdate").append(":")
                            .append(subprop.toString());

                }
                for (P subprop : cris.getAnagrafica4view().get(
                        "awardsfreetext"))
                {

                    sb.append("###").append("awardsfreetext").append(":")
                            .append(subprop.toString());

                }
                for (P subprop : cris.getAnagrafica4view().get(
                        "awardscategory"))
                {

                    sb.append("###").append("awardscategory").append(":")
                            .append(subprop.toString());

                }

                String awardsWith = "";
                for (String s : temporaryAwardsWith)
                {
                    awardsWith += s + "###";
                }

                String value = sb + "|#|#|#" + awardsWith;

                WidgetTesto widget = new WidgetTesto();
                TP propDef;     
                
                P prop;
                try
                {
                    propDef = cris.getClassPropertiesDefinition().newInstance();
                    propDef.setRendering(widget);                    
                    prop = cris.getClassProperty().newInstance();
                    TextValue avalue = new TextValue();
                    avalue.setReal(value);
                    prop.setValue(avalue);
                    prop.setVisibility(VisibilityConstants.PUBLIC);
                    prop.setTypo(propDef);
                    results.add(prop);
                }
                catch (InstantiationException e)
                {
                    log.error(e.getMessage(), e);
                }
                catch (IllegalAccessException e)
                {
                    log.error(e.getMessage(), e);
                }
               

            }
        }
        return results;

    }
}
