package ua.edu.sumdu.essuir.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.hpl.jena.sparql.function.library.print;

import javax.persistence.*;

@Entity
public class Speciality {
    @Id
    @Column(name = "id")
    @JsonProperty("id")
    private Integer id;

    @Column(name = "name")
    @JsonIgnore
    private String name;

    @Column(name = "code")
//    @JsonProperty("code")
    @JsonIgnore
    private String code;

    @OneToOne
    @JoinColumn(name = "chair_id")
    @JsonBackReference
    private ChairEntity chairEntity;

    public Speciality() {
    }

    private Speciality(Builder builder) {
        id = builder.id;
        name = builder.name;
        code = builder.code;
        chairEntity = builder.chairEntity;
    }

    @JsonProperty("name")
    public String getComplexName() {
        return String.format("%s - %s", code, name);
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public ChairEntity getChairEntity() {
        return chairEntity;
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
            this.chairEntity = copy.getChairEntity();
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
