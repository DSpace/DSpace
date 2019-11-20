package org.ssu.entity;

import org.dspace.eperson.EPerson;
import org.ssu.entity.response.DepositorSimpleUnit;

import javax.persistence.Column;

public class EssuirEperson implements DepositorSimpleUnit {
    private EPerson ePerson;
    private String position;
    private ChairEntity chairEntity;

    private EssuirEperson(Builder builder) {
        ePerson = builder.ePerson;
        position = builder.position;
        chairEntity = builder.chairEntity;
    }

    public String getPosition() {
        return position;
    }

    public ChairEntity getChairEntity() {
        return chairEntity;
    }

    @Override
    public String getName() {
        return getLastName() + " " + getFirstName();
    }

    public String getLastName() {
        return ePerson.getLastName();
    }

    public String getFirstName() {
        return ePerson.getFirstName();
    }

    public String getEmail() {
        return  ePerson.getEmail();
    }
    public String getNetid() {
        return ePerson.getNetid();
    }

    public static final class Builder {
        private EPerson ePerson;
        private String position;
        private ChairEntity chairEntity;

        public Builder() {
        }

        public Builder(EssuirEperson copy) {
            this.ePerson = copy.ePerson;
            this.position = copy.position;
            this.chairEntity = copy.chairEntity;
        }

        public Builder withEPerson(EPerson ePerson) {
            this.ePerson = ePerson;
            return this;
        }

        public Builder withPosition(String position) {
            this.position = position;
            return this;
        }

        public Builder withChairEntity(ChairEntity chairEntity) {
            this.chairEntity = chairEntity;
            return this;
        }

        public EssuirEperson build() {
            return new EssuirEperson(this);
        }
    }
}
