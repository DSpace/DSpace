package org.ssu.repository;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.ssu.entity.AuthorLocalization;
import org.ssu.service.EpersonService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AuthorLocalizationRepository {
    private static final org.ssu.entity.jooq.Authors AUTHORS = org.ssu.entity.jooq.Authors.TABLE;

    @Resource
    private DSLContext dsl;

    @Resource
    private EpersonService epersonService;

    @Resource
    private DspaceObjectRepository dspaceObjectRepository;

    public void deleteByUuid(UUID uuid) {
        dsl.delete(AUTHORS)
                .where(AUTHORS.uuid.eq(uuid))
                .execute();
    }
    public void updateAuthorOrcid(AuthorLocalization author) {
        dsl.update(AUTHORS)
                .set(AUTHORS.orcid, author.getOrcid())
                .where(AUTHORS.uuid.eq(author.getUuid()))
                .execute();
    }
    public List<AuthorLocalization> findAll() {
        return dsl.selectFrom(AUTHORS)
                .fetch()
                .stream()
                .map(author ->
                        new AuthorLocalization()
                                .addAuthorData(author.get(AUTHORS.surnameEnglish), author.get(AUTHORS.initialsEnglish), Locale.ENGLISH)
                                .addAuthorData(author.get(AUTHORS.surnameRussian), author.get(AUTHORS.initialsRussian), Locale.forLanguageTag("ru"))
                                .addAuthorData(author.get(AUTHORS.surnameUkrainian), author.get(AUTHORS.initialsUkrainian), Locale.forLanguageTag("uk"))
                                .setOrcid(author.get(AUTHORS.orcid))
                                .setUuid(author.get(AUTHORS.uuid))

                )
                .collect(Collectors.toList());
    }

    private UUID resolveEpersonUuidIfUserExists(AuthorLocalization author) {
        return Stream.of(
                epersonService.findEpersonUuuidByPersonalName(author.getInitials(Locale.ENGLISH), author.getSurname(Locale.ENGLISH)),
                epersonService.findEpersonUuuidByPersonalName(author.getInitials(Locale.forLanguageTag("ru")), author.getSurname(Locale.forLanguageTag("ru"))),
                epersonService.findEpersonUuuidByPersonalName(author.getInitials(Locale.forLanguageTag("uk")), author.getSurname(Locale.forLanguageTag("uk"))))
                .filter(Optional::isPresent)
                .reduce((a, b) -> a)
                .map(Optional::get)
                .orElse(author.getUuid());
    }

    public void updateAuthorData(AuthorLocalization author) {

        dspaceObjectRepository.insertUuid(resolveEpersonUuidIfUserExists(author));

        dsl.insertInto(AUTHORS)
                .set(AUTHORS.surnameEnglish, author.getSurname(Locale.ENGLISH))
                .set(AUTHORS.initialsEnglish, author.getInitials(Locale.ENGLISH))
                .set(AUTHORS.surnameRussian, author.getSurname(Locale.forLanguageTag("ru")))
                .set(AUTHORS.initialsRussian, author.getInitials(Locale.forLanguageTag("ru")))
                .set(AUTHORS.surnameUkrainian, author.getSurname(Locale.forLanguageTag("uk")))
                .set(AUTHORS.initialsUkrainian, author.getInitials(Locale.forLanguageTag("uk")))
                .set(AUTHORS.orcid, author.getOrcid())
                .set(AUTHORS.uuid, author.getUuid())
                .onDuplicateKeyUpdate()
                .set(AUTHORS.surnameEnglish, author.getSurname(Locale.ENGLISH))
                .set(AUTHORS.initialsEnglish, author.getInitials(Locale.ENGLISH))
                .set(AUTHORS.surnameRussian, author.getSurname(Locale.forLanguageTag("ru")))
                .set(AUTHORS.initialsRussian, author.getInitials(Locale.forLanguageTag("ru")))
                .set(AUTHORS.surnameUkrainian, author.getSurname(Locale.forLanguageTag("uk")))
                .set(AUTHORS.initialsUkrainian, author.getInitials(Locale.forLanguageTag("uk")))
                .set(AUTHORS.orcid, author.getOrcid())
                .execute();
    }
}
