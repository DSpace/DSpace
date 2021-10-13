/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.v1;

/**
 * POJO representation for a SHERPA journal
 * 
 * @author Andrea Bollini
 * 
 */
@Deprecated
public class SHERPAJournal
{
    private String title;

    private String issn;

    private String zetopub;

    private String romeopub;

    public SHERPAJournal() {

    }

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

    public void setTitle(String title) {
        this.title = title;
    }

    public void setIssn(String issn) {
        this.issn = issn;
    }

    public void setZetopub(String zetopub) {
        this.zetopub = zetopub;
    }

    public void setRomeopub(String romeopub) {
        this.romeopub = romeopub;
    }
}
