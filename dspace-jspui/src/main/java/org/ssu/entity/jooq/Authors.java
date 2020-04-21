package org.ssu.entity.jooq;

import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Authors extends TableImpl<Record> {
    public static final Authors TABLE = new Authors();

    public final TableField<Record, String> surnameEnglish = createField("surname_en", SQLDataType.VARCHAR(32));
    public final TableField<Record, String> initialsEnglish = createField("initials_en", SQLDataType.VARCHAR(64));
    public final TableField<Record, String> surnameRussian = createField("surname_ru", SQLDataType.VARCHAR(32));
    public final TableField<Record, String> initialsRussian = createField("initials_ru", SQLDataType.VARCHAR(64));
    public final TableField<Record, String> surnameUkrainian = createField("surname_uk", SQLDataType.VARCHAR(32));
    public final TableField<Record, String> initialsUkrainian = createField("initials_uk", SQLDataType.VARCHAR(64));
    public final TableField<Record, String> orcid = createField("orcid", SQLDataType.VARCHAR(100));
    public final TableField<Record, UUID> uuid = createField("uuid", SQLDataType.UUID.identity(true));
    public final UniqueKey<Record> primaryKey = Internal.createUniqueKey(TABLE, uuid);

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


    @Override
    public UniqueKey<Record> getPrimaryKey() {
        return primaryKey;
    }

    @Override
    public List<UniqueKey<Record>> getKeys() {
        return Collections.singletonList(primaryKey);
    }
}