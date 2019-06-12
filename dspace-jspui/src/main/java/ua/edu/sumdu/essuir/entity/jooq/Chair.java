package ua.edu.sumdu.essuir.entity.jooq;

import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

public class Chair extends TableImpl<Record> {
    public static final Chair TABLE = new Chair();

    public final TableField<Record, Integer> chairId = createField("chair_id", SQLDataType.INTEGER);
    public final TableField<Record, String> chairName = createField("chair_name", SQLDataType.VARCHAR(256));
    public final TableField<Record, Integer> facultyId = createField("faculty_id", SQLDataType.INTEGER);

    public Chair() {
        super("chair");
    }

    public Chair(Chair item, String alias) {
        super(alias, null, item);
    }

    @Override
    public Chair as(String alias) {
        return new Chair(this, alias);
    }
}
