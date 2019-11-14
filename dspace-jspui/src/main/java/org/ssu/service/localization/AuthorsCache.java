package org.ssu.service.localization;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.ssu.entity.AuthorLocalization;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AuthorsCache {
    private static final org.ssu.entity.jooq.Authors AUTHORS = org.ssu.entity.jooq.Authors.TABLE;

    @Resource
    private DSLContext dsl;
    private List<AuthorLocalization> authorLocalizations = new ArrayList<>();
    private Map<String, AuthorLocalization> englishMapping;
    private Map<String, AuthorLocalization> russianMapping;
    private Map<String, AuthorLocalization> ukrainianMapping;

    @PostConstruct
    public void updateCache() {
        authorLocalizations = dsl.selectFrom(AUTHORS)
                .fetch()
                .stream()
                .map(author ->
                        new AuthorLocalization()
                                .addAuthorData(author.get(AUTHORS.surnameEnglish), author.get(AUTHORS.initialsEnglish), Locale.ENGLISH)
                                .addAuthorData(author.get(AUTHORS.surnameRussian), author.get(AUTHORS.initialsRussian), Locale.forLanguageTag("ru"))
                                .addAuthorData(author.get(AUTHORS.surnameUkrainian), author.get(AUTHORS.initialsUkrainian), Locale.forLanguageTag("uk"))
                                .setOrcid(author.get(AUTHORS.orcid))
                )
                .collect(Collectors.toList());

        englishMapping = authorLocalizations.stream()
                .collect(Collectors.toMap(author -> author.getFormattedAuthorData("%s, %s", Locale.ENGLISH), author -> author, (a, b) -> a));

        russianMapping = authorLocalizations.stream()
                .collect(Collectors.toMap(author -> author.getFormattedAuthorData("%s, %s", Locale.forLanguageTag("ru")), author -> author, (a, b) -> a));

        ukrainianMapping = authorLocalizations.stream()
                .collect(Collectors.toMap(author -> author.getFormattedAuthorData("%s, %s", Locale.forLanguageTag("uk")), author -> author, (a, b) -> a));
    }

    public boolean isAuthorLocalizationPresent(String author) {
        return Stream.of(englishMapping.get(author), ukrainianMapping.get(author), russianMapping.get(author)).anyMatch(Objects::nonNull);
    }

    public AuthorLocalization getAuthorLocalization(String name) {
        List<String> authorData = Arrays.asList(name.split(","));
        String surname = authorData.get(0).trim();
        String initials = authorData.size() > 1 ? authorData.get(1).trim() : "";

        AuthorLocalization defaultAuthorLocalization = new AuthorLocalization()
                .addAuthorData(surname, initials, Locale.ENGLISH)
                .addAuthorData(surname, initials, Locale.forLanguageTag("ru"))
                .addAuthorData(surname, initials, Locale.forLanguageTag("uk"));

        return Stream.of(englishMapping.get(name), ukrainianMapping.get(name), russianMapping.get(name))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(defaultAuthorLocalization);
    }

    public void updateAuthorOrcid(AuthorLocalization author) {
        dsl.update(AUTHORS)
                .set(AUTHORS.orcid, author.getOrcid())
                .where(AUTHORS.initialsEnglish.eq(author.getInitials(Locale.ENGLISH)).and(AUTHORS.surnameEnglish.eq(author.getSurname(Locale.ENGLISH))))
                .execute();
        updateCache();
    }

    public void removeAuthorData(AuthorLocalization author) {
        dsl.delete(AUTHORS)
                .where(AUTHORS.initialsEnglish.eq(author.getInitials(Locale.ENGLISH)).and(AUTHORS.surnameEnglish.eq(author.getSurname(Locale.ENGLISH))))
                .execute();
        updateCache();
    }

    public void updateAuthorData(AuthorLocalization author) {
        dsl.insertInto(AUTHORS)
                .set(AUTHORS.surnameEnglish, author.getSurname(Locale.ENGLISH))
                .set(AUTHORS.initialsEnglish, author.getInitials(Locale.ENGLISH))
                .set(AUTHORS.surnameRussian, author.getSurname(Locale.forLanguageTag("ru")))
                .set(AUTHORS.initialsRussian, author.getInitials(Locale.forLanguageTag("ru")))
                .set(AUTHORS.surnameUkrainian, author.getSurname(Locale.forLanguageTag("uk")))
                .set(AUTHORS.initialsUkrainian, author.getInitials(Locale.forLanguageTag("uk")))
                .set(AUTHORS.orcid, author.getOrcid())
                .onDuplicateKeyUpdate()
                .set(AUTHORS.surnameRussian, author.getSurname(Locale.forLanguageTag("ru")))
                .set(AUTHORS.initialsRussian, author.getInitials(Locale.forLanguageTag("ru")))
                .set(AUTHORS.surnameUkrainian, author.getSurname(Locale.forLanguageTag("uk")))
                .set(AUTHORS.initialsUkrainian, author.getInitials(Locale.forLanguageTag("uk")))
                .set(AUTHORS.orcid, author.getOrcid())
                .execute();
        updateCache();
    }
    public List<AuthorLocalization> getAuthors() {
        return new ArrayList<>(englishMapping.values());
    }
}
