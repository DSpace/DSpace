package org.ssu.service;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.ssu.entity.AuthorLocalization;
import org.ssu.service.localization.AuthorsCache;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class AuthorsService {
    @Resource
    private AuthorsCache authorsCache;

    public void updateAuthorOrcid(AuthorLocalization author) {
        authorsCache.updateAuthorOrcid(author);
    }

    public AuthorLocalization getAuthorLocalization(Optional<String> authorName) {
        return authorName.map(s -> authorsCache.getAuthorLocalization(s)).orElse(null);
    }

    public AuthorLocalization getAuthorLocalization(String authorName) {
        return authorsCache.getAuthorLocalization(authorName);
    }

    public List<AuthorLocalization> getAllAuthors(Optional<String> startsWith) {
        Predicate<AuthorLocalization> isCurrentAuthorSurnameStartsWith = (author) ->
                startsWith.isPresent() && (
                        author.getSurname(Locale.ENGLISH).toLowerCase().startsWith(startsWith.get().toLowerCase()) ||
                                author.getSurname(Locale.forLanguageTag("ru")).toLowerCase().startsWith(startsWith.get().toLowerCase()) ||
                                author.getSurname(Locale.forLanguageTag("uk")).toLowerCase().startsWith(startsWith.get().toLowerCase()));

        return authorsCache.getAuthors()
                .stream()
                .filter(author -> !startsWith.isPresent() || isCurrentAuthorSurnameStartsWith.test(author))
                .filter(author -> StringUtils.isNotEmpty(author.getSurname(Locale.ENGLISH)))
                .sorted(Comparator.comparing(another -> another.getSurname(Locale.ENGLISH)))

                .collect(Collectors.toList());
    }

    public void updateAuthorData(AuthorLocalization author) {
        authorsCache.updateAuthorData(author);
    }

    public void removeAuthorData(String author) {
        removeAuthorData(authorsCache.getAuthorLocalization(author));
    }

    public void removeAuthorData(AuthorLocalization author) {
        authorsCache.removeAuthorData(author);
    }

    public boolean isAuthorLocalizationPresent(String author) {
        return authorsCache.isAuthorLocalizationPresent(author);
    }
}
