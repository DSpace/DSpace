package org.ssu.service.localization;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.ssu.entity.AuthorLocalization;
import org.ssu.repository.AuthorLocalizationRepository;
import org.ssu.repository.DspaceObjectRepository;
import org.ssu.service.EpersonService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AuthorsCache {
    @Resource
    private AuthorLocalizationRepository authorLocalizationRepository;

    private List<AuthorLocalization> authorLocalizations = new ArrayList<>();
    private Map<String, AuthorLocalization> englishMapping;
    private Map<String, AuthorLocalization> russianMapping;
    private Map<String, AuthorLocalization> ukrainianMapping;

    @PostConstruct
    public void updateCache() {
        authorLocalizations = authorLocalizationRepository.findAll();

        englishMapping = authorLocalizations.stream()
                .collect(Collectors.toMap(author -> author.getFormattedAuthorData("%s, %s", Locale.ENGLISH), author -> author, (a, b) -> a));

        russianMapping = authorLocalizations.stream()
                .collect(Collectors.toMap(author -> author.getFormattedAuthorData("%s, %s", Locale.forLanguageTag("ru")), author -> author, (a, b) -> a));

        ukrainianMapping = authorLocalizations.stream()
                .collect(Collectors.toMap(author -> author.getFormattedAuthorData("%s, %s", Locale.forLanguageTag("uk")), author -> author, (a, b) -> a));
    }

    public Optional<AuthorLocalization> getAuthor(UUID uuid) {
        return authorLocalizations.stream()
                .filter(author -> author.getUuid().equals(uuid))
                .findFirst();
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

    public List<AuthorLocalization> getAuthors() {
        return new ArrayList<>(englishMapping.values());
    }
}
