package ua.edu.sumdu.essuir.entity.jooq;

import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

public class Faculty extends TableImpl<Record> {
    public static final Faculty TABLE = new Faculty();

    public final TableField<Record, Integer> facultyId = createField("faculty_id", SQLDataType.INTEGER);
    public final TableField<Record, String> facultyName = createField("faculty_name", SQLDataType.VARCHAR(128));

    public Faculty() {
        super("faculty");
    }

    public Faculty(Faculty item, String alias) {
        super(alias, null, item);
    }

    @Override
    public Faculty as(String alias) {
        return new Faculty(this, alias);
    }
}
