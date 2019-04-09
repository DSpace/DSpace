/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws.marshaller;


import java.util.List;

import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.util.UtilsXSD;
import org.dspace.app.cris.ws.marshaller.bean.WSItem;
import org.dspace.app.cris.ws.marshaller.bean.WSMetadata;
import org.dspace.app.cris.ws.marshaller.bean.WSMetadataValue;
import org.jdom.Element;
import org.jdom.Namespace;

public class MarshallerPublications implements Marshaller<WSItem>
{
    @Override
    public Element buildResponse(List<WSItem> docList, long start, long hit,
            String type, String[] splitProjection, boolean showHiddenMetadata, String nameRoot)
    {
        
        Namespace echoNamespaceRoot = Namespace.getNamespace("cris",
        		UtilsXSD.NAMESPACE_CRIS);
        Element root = new Element(nameRoot, echoNamespaceRoot);
        root.setAttribute("hit", "" + hit);
        root.setAttribute("start", "" + start);
        root.setAttribute("rows", "" + docList.size());        
        root.setAttribute("type", "" + type);
        
        // build the response XML with JDOM
        Namespace echoNamespace = Namespace.getNamespace(UtilsXSD.NAMESPACE_PREFIX_ITEM,
        		UtilsXSD.NAMESPACE_ITEM);

        Element child = new Element("crisobjects", echoNamespace);     
                    

        for (WSItem doc : docList)
        {
            Element row = new Element("crisobject", echoNamespace);
            row.setAttribute("handle", doc.getHandle());
            row.setAttribute("itemID", "" + doc.getItemID());

            Element communities = new Element("communities", echoNamespace);
            if (doc.getCommunity() != null && !doc.getCommunity().isEmpty())
            {
                int index = 0;
                for (String ccc : doc.getCommunity())
                {
                    Element community = new Element("community",
                            echoNamespace);
                    
                    Element communityName = new Element("name", echoNamespace);
                    Element communityHandle = new Element("handle",
                            echoNamespace);
                    
                    communityName.addContent(doc.getCommunityName().get(index));
                    communityHandle.addContent(doc.getCommunityHandle().get(
                            index));
                    community.setAttribute("id", ccc);
                    community.addContent(communityName);
                    community.addContent(communityHandle);
                    communities.addContent(community);
                    index++;
                }
            }
            row.addContent(communities);
            Element collections = new Element("collections", echoNamespace);
            if (doc.getCollection() != null && !doc.getCollection().isEmpty())
            {
                int index = 0;
                for (String ccc : doc.getCollection())
                {
                    Element collection = new Element("collection",
                            echoNamespace);
                    
                    Element collectionName = new Element("name", echoNamespace);
                    Element collectionHandle = new Element("handle",
                            echoNamespace);
                    
                    collectionName.addContent(doc.getCollectionName()
                            .get(index));
                    collectionHandle.addContent(doc.getCollectionHandle().get(
                            index));
                    collection.setAttribute("id", ccc);
                    collection.addContent(collectionName);
                    collection.addContent(collectionHandle);
                    collections.addContent(collection);
                    index++;
                }
            }
            row.addContent(collections);

            Element metadataItem = new Element("metadataitem", echoNamespace);
            
            List<WSMetadata> fieldsName = doc.getMetadata();
            for (WSMetadata field : fieldsName)
            {
                if (!field.getName().startsWith(ConstantMetrics.PREFIX_FIELD))
                {
                    Element metadata = new Element("metadata", echoNamespace);

                    Element term = new Element("term", echoNamespace);
                    term.addContent(field.getName());

                    Element values = new Element("values", echoNamespace);

                    for (WSMetadataValue mValue : field.getValues())
                    {

                        Element value = new Element("value", echoNamespace);
                        if (mValue.getAuthority() != null
                                && !mValue.getAuthority().isEmpty())
                        {
                            value.setAttribute("authority", mValue.getAuthority());
                        }

                        value.setAttribute("place", "" + mValue.getPlace());
                        if (mValue.getShare() != null)
                        {
                            value.setAttribute("share", "" + mValue.getShare());
                        }

                        value.addContent(mValue.getValue());

                        values.addContent(value);

                    }

                    metadata.addContent(term);
                    metadata.addContent(values);
                    metadataItem.addContent(metadata);
                }
            }
            row.addContent(metadataItem);

            Element metrics = new Element("metrics", echoNamespace);
            for (WSMetadata field : fieldsName)
            {
                String fieldMetadataName = field.getName();
                if (fieldMetadataName.startsWith(ConstantMetrics.PREFIX_FIELD)
                        && !fieldMetadataName.endsWith(ConstantMetrics.STATS_INDICATOR_TYPE_TIME)
                        && !fieldMetadataName.endsWith(ConstantMetrics.STATS_INDICATOR_TYPE_STARTTIME)
                        && !fieldMetadataName.endsWith(ConstantMetrics.STATS_INDICATOR_TYPE_ENDTIME))
                {
                    Element metric = new Element("metric", echoNamespace);
                    String metricName = fieldMetadataName.replace(ConstantMetrics.PREFIX_FIELD, "");
                    metric.setAttribute("type", metricName);

                    Element value = new Element("value", echoNamespace);
                    value.addContent(field.getValues().get(0).getValue());
                    metric.addContent(value);

                    for (WSMetadata subField : fieldsName)
                    {
                        String subFieldMetadataName = subField.getName();
                        if (subFieldMetadataName.equals(ConstantMetrics.PREFIX_FIELD + metricName + ConstantMetrics.STATS_INDICATOR_TYPE_TIME))
                        {
                            Element subValue = new Element("observation_time", echoNamespace);
                            subValue.addContent(subField.getValues().get(0).getValue());
                            metric.addContent(subValue);
                        }
                        else if (subFieldMetadataName.equals(ConstantMetrics.PREFIX_FIELD + metricName + ConstantMetrics.STATS_INDICATOR_TYPE_STARTTIME))
                        {
                            Element subValue = new Element("start_time", echoNamespace);
                            subValue.addContent(subField.getValues().get(0).getValue());
                            metric.addContent(subValue);
                        }
                        else if (subFieldMetadataName.equals(ConstantMetrics.PREFIX_FIELD + metricName + ConstantMetrics.STATS_INDICATOR_TYPE_ENDTIME))
                        {
                            Element subValue = new Element("end_time", echoNamespace);
                            subValue.addContent(subField.getValues().get(0).getValue());
                            metric.addContent(subValue);
                        }
                    }
                    metrics.addContent(metric);
                }
            }
            row.addContent(metrics);

            child.addContent(row);
        }
        root.addContent(child);
        return root;
    }

}
