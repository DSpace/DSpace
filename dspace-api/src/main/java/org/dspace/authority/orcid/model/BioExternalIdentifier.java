/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.model;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class BioExternalIdentifier {


    protected String id_orcid;
    protected String id_common_name;
    protected String id_reference;
    protected String id_url;

    public BioExternalIdentifier(String id_orcid, String id_common_name, String id_reference, String id_url) {
        this.id_orcid = id_orcid;
        this.id_common_name = id_common_name;
        this.id_reference = id_reference;
        this.id_url = id_url;
    }

    public String getId_orcid() {
        return id_orcid;
    }

    public void setId_orcid(String id_orcid) {
        this.id_orcid = id_orcid;
    }

    public String getId_common_name() {
        return id_common_name;
    }

    public void setId_common_name(String id_common_name) {
        this.id_common_name = id_common_name;
    }

    public String getId_reference() {
        return id_reference;
    }

    public void setId_reference(String id_reference) {
        this.id_reference = id_reference;
    }

    public String getId_url() {
        return id_url;
    }

    public void setId_url(String id_url) {
        this.id_url = id_url;
    }

    @Override
    public String toString() {
        return "BioExternalIdentifier{" +
                "id_orcid='" + id_orcid + '\'' +
                ", id_common_name='" + id_common_name + '\'' +
                ", id_reference='" + id_reference + '\'' +
                ", id_url='" + id_url + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BioExternalIdentifier that = (BioExternalIdentifier) o;

        if (id_common_name != null ? !id_common_name.equals(that.id_common_name) : that.id_common_name != null) {
            return false;
        }
        if (id_orcid != null ? !id_orcid.equals(that.id_orcid) : that.id_orcid != null) {
            return false;
        }
        if (id_reference != null ? !id_reference.equals(that.id_reference) : that.id_reference != null) {
            return false;
        }
        if (id_url != null ? !id_url.equals(that.id_url) : that.id_url != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id_orcid != null ? id_orcid.hashCode() : 0;
        result = 31 * result + (id_common_name != null ? id_common_name.hashCode() : 0);
        result = 31 * result + (id_reference != null ? id_reference.hashCode() : 0);
        result = 31 * result + (id_url != null ? id_url.hashCode() : 0);
        return result;
    }
}
