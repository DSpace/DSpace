package org.ssu.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Entity
@Table(name = "eperson")
public class EPerson implements Depositor{
    @Id
    @Column(name = "eperson_id")
    private Integer id;

    @Column(name = "email")
    private String email;

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "lastname")
    private String lastname;

    @OneToOne
    @JoinColumn(name = "chair_id")
    private ChairEntity chairEntity;

    private EPerson(Builder builder) {
        setId(builder.id);
        setEmail(builder.email);
        firstname = builder.firstname;
        lastname = builder.lastname;
        setChairEntity(builder.chairEntity);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getName() {
        String name = Stream.of(Optional.ofNullable(lastname), Optional.ofNullable(firstname))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining(" "));
        return name.isEmpty() ? email : String.format("%s (%s)", name, email);
    }

    public ChairEntity getChairEntity() {
        return chairEntity;
    }

    public void setChairEntity(ChairEntity chairEntity) {
        this.chairEntity = chairEntity;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        EPerson ePerson = (EPerson) o;

        return new EqualsBuilder()
                .append(id, ePerson.id)
                .append(email, ePerson.email)
                .append(firstname, ePerson.firstname)
                .append(lastname, ePerson.lastname)
                .append(chairEntity, ePerson.chairEntity)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(email)
                .append(firstname)
                .append(lastname)
                .append(chairEntity)
                .toHashCode();
    }

    public static final class Builder {
        private Integer id;
        private String email;
        private String firstname;
        private String lastname;
        private ChairEntity chairEntity;

        public Builder() {
        }

        public Builder(EPerson copy) {
            this.id = copy.getId();
            this.email = copy.getEmail();
            this.firstname = copy.getFirstname();
            this.lastname = copy.getLastname();
            this.chairEntity = copy.getChairEntity();
        }

        public Builder withId(Integer id) {
            this.id = id;
            return this;
        }

        public Builder withEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder withFirstname(String firstname) {
            this.firstname = firstname;
            return this;
        }

        public Builder withLastname(String lastname) {
            this.lastname = lastname;
            return this;
        }

        public Builder withChairEntity(ChairEntity chairEntity) {
            this.chairEntity = chairEntity;
            return this;
        }

        public EPerson build() {
            return new EPerson(this);
        }
    }
}