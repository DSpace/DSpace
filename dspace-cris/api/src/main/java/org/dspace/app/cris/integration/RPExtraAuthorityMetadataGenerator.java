package org.dspace.app.cris.integration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.Choice;

/**
 * 
 * Generic generator to work on nested/simple metadata
 * 
 * @author Pascarelli Luigi Andrea
 *
 */
public class RPExtraAuthorityMetadataGenerator
        implements RPAuthorityExtraMetadataGenerator
{

    private String relatedInputformMetadata = "dc_contributor_department";

    private String schema = "affiliation";

    private String element = "affiliationorgunit";

    private String qualifier = "name";

    //use with aggregate mode
    private boolean singleResultOnAggregate = true;

    @Override
    public Map<String, String> build(ResearcherPage rp)
    {
        // only single result is supported
        Map<String, String> extras = new HashMap<String, String>();
        buildSingleExtraByRP(rp, extras);
        return extras;
    }

    @Override
    public List<Choice> buildAggregate(ResearcherPage rp)
    {
        List<Choice> choiceList = new LinkedList<Choice>();
        if (isSingleResultOnAggregate())
        {
            Map<String, String> extras = new HashMap<String, String>();
            buildSingleExtraByRP(rp, extras);
            choiceList.add(
                    new Choice(ResearcherPageUtils.getPersistentIdentifier(rp),
                            rp.getFullName(),
                            ResearcherPageUtils.getLabel(rp.getFullName(), rp),
                            extras));
        }
        else
        {
            Metadatum[] metadatas = rp.getMetadata(getSchema(), getElement(),
                    getQualifier(), Item.ANY);
            for (Metadatum mm : metadatas)
            {
                Map<String, String> extras = new HashMap<String, String>();
                buildSingleExtraByMetadata(mm, extras);
                choiceList.add(new Choice(
                        ResearcherPageUtils.getPersistentIdentifier(rp),
                        ResearcherPageUtils.getLabel(rp.getFullName(), rp)  + "(" + mm.value + ")",
                        rp.getFullName(),
                        extras));
            }
            // manage value to empty html element
            if (metadatas == null || metadatas.length == 0)
            {
                Map<String, String> extras = new HashMap<String, String>();
                extras.put("data-" + getRelatedInputformMetadata(), "");
                choiceList.add(new Choice(
                        ResearcherPageUtils.getPersistentIdentifier(rp),
                        rp.getFullName(),
                        ResearcherPageUtils.getLabel(rp.getFullName(), rp),
                        extras));
            }
        }
        return choiceList;
    }

    protected void buildSingleExtraByMetadata(Metadatum mm,
            Map<String, String> extras)
    {
        if (mm == null)
        {
            extras.put("data-" + getRelatedInputformMetadata(), "");
        }
        else
        {
            if (StringUtils.isNotBlank(mm.authority))
            {
                extras.put("data-" + getRelatedInputformMetadata(),
                        mm.value + "::" + mm.authority);
            }
            else
            {
                extras.put("data-" + getRelatedInputformMetadata(), mm.value);
            }
        }
    }

    protected void buildSingleExtraByRP(ResearcherPage rp,
            Map<String, String> extras)
    {
        Metadatum mm = rp.getMetadatumFirstValue(getSchema(), getElement(),
                getQualifier(), Item.ANY);
        buildSingleExtraByMetadata(mm, extras);
    }

    public String getRelatedInputformMetadata()
    {
        return relatedInputformMetadata;
    }

    public void setRelatedInputformMetadata(String relatedInputformMetadata)
    {
        this.relatedInputformMetadata = relatedInputformMetadata;
    }

    public String getElement()
    {
        return element;
    }

    public void setElement(String element)
    {
        this.element = element;
    }

    public String getQualifier()
    {
        return qualifier;
    }

    public void setQualifier(String qualifier)
    {
        this.qualifier = qualifier;
    }

    public String getSchema()
    {
        return schema;
    }

    public void setSchema(String schema)
    {
        this.schema = schema;
    }

    public boolean isSingleResultOnAggregate()
    {
        return singleResultOnAggregate;
    }

    public void setSingleResultOnAggregate(boolean singleResultOnAggregate)
    {
        this.singleResultOnAggregate = singleResultOnAggregate;
    }

}
