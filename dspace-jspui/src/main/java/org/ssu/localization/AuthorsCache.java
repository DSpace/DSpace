package org.ssu.localization;

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
                .collect(Collectors.toMap(author -> String.format("%s, %s", author.getSurname(Locale.ENGLISH), author.getInitials(Locale.ENGLISH)), author -> author, (a, b) -> a));

        russianMapping = authorLocalizations.stream()
                .collect(Collectors.toMap(author -> String.format("%s, %s", author.getSurname(Locale.forLanguageTag("ru")), author.getInitials(Locale.forLanguageTag("ru"))), author -> author, (a, b) -> a));

        ukrainianMapping = authorLocalizations.stream()
                .collect(Collectors.toMap(author -> String.format("%s, %s", author.getSurname(Locale.forLanguageTag("uk")), author.getInitials(Locale.forLanguageTag("uk"))), author -> author, (a, b) -> a));
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
}
