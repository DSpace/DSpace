package org.ssu.service;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.ssu.entity.AuthorLocalization;

import javax.annotation.Resource;
import java.util.Locale;

@Service
public class AuthorsService {
    private static final org.ssu.entity.jooq.Authors AUTHORS = org.ssu.entity.jooq.Authors.TABLE;

    @Resource
    private DSLContext dsl;

    public void updateAuthorOrcid(AuthorLocalization author) {
        dsl.update(AUTHORS)
                .set(AUTHORS.orcid, author.getOrcid())
                .where(AUTHORS.initialsEnglish.eq(author.getInitials(Locale.ENGLISH)).and(AUTHORS.surnameEnglish.eq(author.getSurname(Locale.ENGLISH))))
                .execute();
    }
}
