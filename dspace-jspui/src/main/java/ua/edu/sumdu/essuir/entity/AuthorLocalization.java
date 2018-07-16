package ua.edu.sumdu.essuir.entity;


import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "authors")
public class AuthorLocalization implements Serializable {
    @EmbeddedId
    private AuthorsLocalizationPK authorsLocalizationPK;
    @Column
    private String surname_ru;
    @Column
    private String initials_ru;
    @Column
    private String surname_uk;
    @Column
    private String initials_uk;
    @Column
    private String orcid;

    public String getSurname_en() {
        return authorsLocalizationPK.getSurname_en();
    }

    public void setSurname_en(String surname_en) {
        authorsLocalizationPK.setSurname_en(surname_en);
    }

    public String getInitials_en() {
        return authorsLocalizationPK.getInitials_en();
    }

    public void setInitials_en(String initials_en) {
        authorsLocalizationPK.setInitials_en(initials_en);
    }

    public String getSurname_ru() {
        return surname_ru;
    }

    public void setSurname_ru(String surname_ru) {
        this.surname_ru = surname_ru;
    }

    public String getInitials_ru() {
        return initials_ru;
    }

    public void setInitials_ru(String initials_ru) {
        this.initials_ru = initials_ru;
    }

    public String getSurname_uk() {
        return surname_uk;
    }

    public void setSurname_uk(String surname_uk) {
        this.surname_uk = surname_uk;
    }

    public String getInitials_uk() {
        return initials_uk;
    }

    public void setInitials_uk(String initials_uk) {
        this.initials_uk = initials_uk;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }
}
