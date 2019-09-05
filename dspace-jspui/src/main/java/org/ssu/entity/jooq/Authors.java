package org.ssu.entity.jooq;

import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

public class Authors extends TableImpl<Record> {
    public static final Authors TABLE = new Authors();

    public final TableField<Record, String> surnameEnglish = createField("surname_en", SQLDataType.VARCHAR(32));
    public final TableField<Record, String> initialsEnglish = createField("initials_en", SQLDataType.VARCHAR(64));
    public final TableField<Record, String> surnameRussian = createField("surname_ru", SQLDataType.VARCHAR(32));
    public final TableField<Record, String> initialsRussian = createField("initials_ru", SQLDataType.VARCHAR(64));
    public final TableField<Record, String> surnameUkrainian = createField("surname_uk", SQLDataType.VARCHAR(32));
    public final TableField<Record, String> initialsUkrainian = createField("initials_uk", SQLDataType.VARCHAR(64));
    public final TableField<Record, String> orcid = createField("orcid", SQLDataType.VARCHAR(100));


    public Authors() {
        super("authors");
    }

    public Authors(Authors authors, String alias) {
        super(alias, null, authors);
    }

    @Override
    public Authors as(String alias) {
        return new Authors(this, alias);
    }
}