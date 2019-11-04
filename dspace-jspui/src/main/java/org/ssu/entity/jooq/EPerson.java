package org.ssu.entity.jooq;

import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.util.UUID;

public class EPerson extends TableImpl<Record> {
    public static final EPerson TABLE = new EPerson();

    public final TableField<Record, Integer> epersonId = createField("eperson_id", SQLDataType.INTEGER);
    public final TableField<Record, String> email = createField("email", SQLDataType.VARCHAR(64));
    public final TableField<Record, String> password = createField("password", SQLDataType.VARCHAR(128));
    public final TableField<Record, String> firstname = createField("firstname", SQLDataType.VARCHAR(64));
    public final TableField<Record, String> lastname = createField("lastname", SQLDataType.VARCHAR(64));
    public final TableField<Record, String> position = createField("position", SQLDataType.VARCHAR(128));
    public final TableField<Record, Integer> chairId = createField("chair_id", SQLDataType.INTEGER);
    public final TableField<Record, UUID> uuid = createField("uuid", SQLDataType.UUID);

    public EPerson() {
        super("eperson");
    }

    public EPerson(EPerson eperson, String alias) {
        super(alias, null, eperson);
    }

    @Override
    public EPerson as(String alias) {
        return new EPerson(this, alias);
    }
}
