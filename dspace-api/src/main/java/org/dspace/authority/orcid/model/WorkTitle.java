/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.model;

import java.util.Map;

/**
 * http://support.orcid.org/knowledgebase/articles/118807
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class WorkTitle {

    private String title;
    private String subtitle;
    private Map<String, String> translatedTitles;

    public WorkTitle(String title, String subtitle, Map<String, String> translatedTitles) {
        this.title = title;
        this.subtitle = subtitle;
        this.translatedTitles = translatedTitles;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getTranslatedTitles(String languageCode) {
        return translatedTitles.get(languageCode);
    }

    public void setTranslatedTitle(String languageCode, String translatedTitle) {
        translatedTitles.put(languageCode, translatedTitle);
    }

    @Override
    public String toString() {
        return "WorkTitle{" +
                "title='" + title + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", translatedTitles=" + translatedTitles +
                '}';
    }
}
