package org.dspace.eperson;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.content.DSpaceObject;
import org.dspace.eperson.essuir.DepositorSimpleUnit;

import javax.persistence.*;

@Entity
@Table(name = "speciality")
public class Speciality extends DSpaceObject implements DepositorSimpleUnit {
    @Column(name = "id")
    @JsonProperty("id")
    private Integer specialityId;

    @Column(name = "name")
    @JsonIgnore
    private String name;

    @Column(name = "code")
    @JsonIgnore
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chair_id", referencedColumnName = "chair_id")
    @JsonBackReference
    private ChairEntity chairEntity;

    public Speciality() {
    }

    @Override
    public int getType() {
        return 0;
    }

    private Speciality(Builder builder) {
        specialityId = builder.id;
        name = builder.name;
        code = builder.code;
        chairEntity = builder.chairEntity;
    }

    @JsonProperty("name")
    public String getComplexName() {
        return String.format("%s - %s", code, name);
    }

    public Integer getId() {
        return specialityId;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    @Override
    @JsonIgnore
    public ChairEntity getChair() {
        return chairEntity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Speciality that = (Speciality) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .append(name, that.name)
                .append(code, that.code)
                .append(chairEntity, that.chairEntity)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(name)
                .append(code)
                .append(chairEntity)
                .toHashCode();
    }

    public static final class Builder {
        private Integer id;
        private String name;
        private String code;
        private ChairEntity chairEntity;

        public Builder() {
        }

        public Builder(Speciality copy) {
            this.id = copy.getId();
            this.name = copy.getName();
            this.code = copy.getCode();
            this.chairEntity = copy.getChair();
        }

        public Builder withId(Integer id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withCode(String code) {
            this.code = code;
            return this;
        }

        public Builder withChairEntity(ChairEntity chairEntity) {
            this.chairEntity = chairEntity;
            return this;
        }

        public Speciality build() {
            return new Speciality(this);
        }
    }
}
