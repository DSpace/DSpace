/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa;

/**
 * POJO representation for a SHERPA journal
 * 
 * @author Andrea Bollini
 * 
 */
public class SHERPAJournal
{
    private String title;

    private String issn;

    private String zetopub;

    private String romeopub;

    public SHERPAJournal(String title, String issn, String zetopub,
            String romeopub)
    {
        super();
        this.title = title;
        this.issn = issn;
        this.zetopub = zetopub;
        this.romeopub = romeopub;
    }

    public String getTitle()
    {
        return title;
    }

    public String getIssn()
    {
        return issn;
    }

    public String getZetopub()
    {
        return zetopub;
    }

    public String getRomeopub()
    {
        return romeopub;
    }

}
