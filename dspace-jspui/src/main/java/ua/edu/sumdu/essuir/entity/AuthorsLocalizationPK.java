package ua.edu.sumdu.essuir.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class AuthorsLocalizationPK implements Serializable{
    @Column
    private String surname_en;
    @Column
    private String initials_en;

    public AuthorsLocalizationPK() {
    }

    public String getSurname_en() {
        return surname_en;
    }

    public void setSurname_en(String surname_en) {
        this.surname_en = surname_en;
    }

    public String getInitials_en() {
        return initials_en;
    }

    public void setInitials_en(String initials_en) {
        this.initials_en = initials_en;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        AuthorsLocalizationPK rhs = (AuthorsLocalizationPK) obj;
        return new EqualsBuilder()
                .append(this.surname_en, rhs.surname_en)
                .append(this.initials_en, rhs.initials_en)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(surname_en)
                .append(initials_en)
                .toHashCode();
    }
}
