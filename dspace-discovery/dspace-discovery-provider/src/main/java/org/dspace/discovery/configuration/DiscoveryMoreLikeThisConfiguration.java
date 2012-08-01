/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import org.springframework.beans.factory.annotation.Required;

import java.util.List;

/**
 * Class that contains the more like this configuration on item pages
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DiscoveryMoreLikeThisConfiguration {
    private List<String> similarityMetadataFields;
    private int minTermFrequency;
    private int max;
    private int minWordLength;

    @Required
    public void setSimilarityMetadataFields(List<String> similarityMetadataFields)
    {
        this.similarityMetadataFields = similarityMetadataFields;
    }

    public List<String> getSimilarityMetadataFields()
    {
        return similarityMetadataFields;
    }

    @Required
    public void setMinTermFrequency(int minTermFrequency)
    {
        this.minTermFrequency = minTermFrequency;
    }

    public int getMinTermFrequency()
    {
        return minTermFrequency;
    }

    @Required
    public void setMax(int max)
    {
        this.max = max;
    }

    public int getMax()
    {
        return max;
    }

    public int getMinWordLength()
    {
        return minWordLength;
    }

    public void setMinWordLength(int minWordLength)
    {
        this.minWordLength = minWordLength;
    }
}