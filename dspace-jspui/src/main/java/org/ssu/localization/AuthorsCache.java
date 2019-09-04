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
                .map(author -> new AuthorLocalization.Builder()
                        .withSurnameEnglish(author.get(AUTHORS.surnameEnglish))
                        .withInitialsEnglish(author.get(AUTHORS.initialsEnglish))
                        .withSurnameRussian(author.get(AUTHORS.surnameRussian))
                        .withInitialsRussian(author.get(AUTHORS.initialsRussian))
                        .withSurnameUkrainian(author.get(AUTHORS.surnameUkrainian))
                        .withInitialsUkrainian(author.get(AUTHORS.initialsUkrainian))
                        .withOrcid(author.get(AUTHORS.orcid))
                        .build())
                .collect(Collectors.toList());

        englishMapping = authorLocalizations.stream()
                .collect(Collectors.toMap(author -> String.format("%s, %s", author.getSurnameEnglish(), author.getInitialsEnglish()), author -> author, (a, b) -> a));

        russianMapping = authorLocalizations.stream()
                .collect(Collectors.toMap(author -> String.format("%s, %s", author.getSurnameRussian(), author.getInitialsRussian()), author -> author, (a, b) -> a));

        ukrainianMapping = authorLocalizations.stream()
                .collect(Collectors.toMap(author -> String.format("%s, %s", author.getSurnameUkrainian(), author.getInitialsUkrainian()), author -> author, (a, b) -> a));
    }

    public AuthorLocalization getAuthorLocalization(String name) {
        List<String> authorData = Arrays.asList(name.split(","));
        String surname = authorData.get(0).trim();
        String initials = authorData.size() > 1 ? authorData.get(1).trim() : "";

        AuthorLocalization defaultAuthorLocalization = new AuthorLocalization.Builder()
                .withInitialsEnglish(initials)
                .withInitialsRussian(initials)
                .withInitialsUkrainian(initials)
                .withSurnameEnglish(surname)
                .withSurnameRussian(surname)
                .withSurnameUkrainian(surname)
                .build();

        return Stream.of(englishMapping.get(name), ukrainianMapping.get(name), russianMapping.get(name))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(defaultAuthorLocalization);
    }
}
