/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.v2;

import java.io.Serializable;
import java.util.List;

/**
 * Plain java representation of a SHERPA Journal object, based on SHERPA API v2 responses.
 *
 * In a SHERPA search for journal deposit policies, this is generally structured
 * as a list in the SHERPAResponse object.
 * Each journal contains a list of publisher data and list of publishing policies as well as basic metadata
 * about the journal such as ISSNs, titles, whether it appears in DOAJ, primary publisher, etc.
 * @see SHERPAResponse
 * @see org.dspace.external.provider.impl.SHERPAv2JournalDataProvider
 *
 * @author Kim Shepherd
 */
public class SHERPAJournal implements Serializable {

    private List<String> titles;
    private String url;
    private List<String> issns;
    private String romeoPub;
    private String zetoPub;
    private SHERPAPublisher publisher;
    private List<SHERPAPublisher> publishers;
    private List<SHERPAPublisherPolicy> policies;
    private Boolean inDOAJ;

    public SHERPAJournal() {

    }

    public List<String> getTitles() {
        return titles;
    }

    public void setTitles(List<String> titles) {
        this.titles = titles;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getIssns() {
        return issns;
    }

    public void setIssns(List<String> issns) {
        this.issns = issns;
    }

    public String getRomeoPub() {
        return romeoPub;
    }

    public void setRomeoPub(String romeoPub) {
        this.romeoPub = romeoPub;
    }

    public String getZetoPub() {
        return zetoPub;
    }

    public void setZetoPub(String zetoPub) {
        this.zetoPub = zetoPub;
    }

    public SHERPAPublisher getPublisher() {
        return publisher;
    }

    public void setPublisher(SHERPAPublisher publisher) {
        this.publisher = publisher;
    }

    public List<SHERPAPublisher> getPublishers() {
        return publishers;
    }

    public void setPublishers(List<SHERPAPublisher> publishers) {
        this.publishers = publishers;
    }

    public List<SHERPAPublisherPolicy> getPolicies() {
        return policies;
    }

    public void setPolicies(List<SHERPAPublisherPolicy> policies) {
        this.policies = policies;
    }

    public Boolean getInDOAJ() {
        return inDOAJ;
    }

    public void setInDOAJ(Boolean inDOAJ) {
        this.inDOAJ = inDOAJ;
    }
}
