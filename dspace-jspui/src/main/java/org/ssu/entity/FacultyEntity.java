package org.ssu.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.ssu.entity.response.DepositorDivision;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "faculty")
public class FacultyEntity implements DepositorDivision {
    @Id
    @Column(name = "faculty_id")
    @JsonProperty("id")
    private Integer id;

    @Column(name = "faculty_name")
    @JsonProperty("name")
    private String name;

    @OneToMany(mappedBy = "facultyEntityName")
    @JsonProperty("chairs")
    @JsonManagedReference
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<ChairEntity> chairs;

    private FacultyEntity(Builder builder) {
        setId(builder.id);
        setName(builder.name);
    }

    public FacultyEntity() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ChairEntity> getChairs() {
        return chairs;
    }

    public void setChairs(List<ChairEntity> chairs) {
        this.chairs = chairs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        FacultyEntity that = (FacultyEntity) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .append(name, that.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(name)
                .toHashCode();
    }

    public static final class Builder {
        private Integer id;
        private String name;

        public Builder() {
        }

        public Builder(FacultyEntity copy) {
            this.id = copy.getId();
            this.name = copy.getName();
        }

        public Builder withId(Integer id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public FacultyEntity build() {
            return new FacultyEntity(this);
        }
    }
}