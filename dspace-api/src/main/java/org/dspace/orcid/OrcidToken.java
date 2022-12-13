/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.content.Item;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.EPerson;

/**
 * Entity that stores ORCID access-token related to a given eperson or a given
 * profile item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Entity
@Table(name = "orcid_token")
public class OrcidToken implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orcid_token_id_seq")
    @SequenceGenerator(name = "orcid_token_id_seq", sequenceName = "orcid_token_id_seq", allocationSize = 1)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eperson_id")
    protected EPerson ePerson;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_item_id")
    private Item profileItem;

    @Column(name = "access_token")
    private String accessToken;

    @Override
    public Integer getID() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public EPerson getEPerson() {
        return ePerson;
    }

    public void setEPerson(EPerson eperson) {
        this.ePerson = eperson;
    }

    public Item getProfileItem() {
        return profileItem;
    }

    public void setProfileItem(Item profileItem) {
        this.profileItem = profileItem;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

}
