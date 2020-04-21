package org.ssu.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;

import java.util.*;
import java.util.stream.Stream;

public class AuthorLocalization {

    public class AuthorData  {
        private String surname;
        private String initials;

        public String getSurname() {
            return surname;
        }

        public String getInitials() {
            return initials;
        }

        public AuthorData(String surname, String initials) {
            this.surname = surname;
            this.initials = initials;
        }

        @Override
        public String toString() {
            return "AuthorData{" +
                    "surname='" + surname + '\'' +
                    ", initials='" + initials + '\'' +
                    '}';
        }
    }

    private Map<String, AuthorData> authorLocalizations = new HashMap<>();
    private String orcid;
    private UUID uuid;

    public UUID getUuid() {
        return uuid;
    }

    public String getOrcid() {
        return orcid;
    }

    public AuthorLocalization addAuthorData(String surname, String initials, Locale locale) {
        authorLocalizations.put(locale.getLanguage(), new AuthorData(surname, initials));
        return this;
    }

    public AuthorLocalization setUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public AuthorLocalization setOrcid(String orcid) {
        this.orcid = orcid;
        return this;
    }

    private Locale checkThatLocaleIsSupporting(Locale locale) {
        return Stream.of(Locale.forLanguageTag("uk"), Locale.forLanguageTag("ru"), Locale.ENGLISH)
                .filter(supportedLocale -> supportedLocale.equals(locale))
                .findFirst()
                .orElse(Locale.ENGLISH);
    }

    public String getSurname(Locale locale) {
        return authorLocalizations.get(checkThatLocaleIsSupporting(locale).getLanguage()).getSurname();
    }

    public String getInitials(Locale locale) {
        return authorLocalizations.get(checkThatLocaleIsSupporting(locale).getLanguage()).getInitials();
    }

    public String getFormattedAuthorData(String format, Locale locale) {
        return String.format(format, getSurname(locale), getInitials(locale));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        AuthorLocalization that = (AuthorLocalization) o;

        return new EqualsBuilder()
                .append(getSurname(Locale.ENGLISH), that.getSurname(Locale.ENGLISH))
                .append(getInitials(Locale.ENGLISH), that.getInitials(Locale.ENGLISH))
                .append(orcid, that.orcid)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(authorLocalizations)
                .append(orcid)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "AuthorLocalization{" +
                "authorLocalizations=" + authorLocalizations +
                ", orcid='" + orcid + '\'' +
                '}';
    }
}