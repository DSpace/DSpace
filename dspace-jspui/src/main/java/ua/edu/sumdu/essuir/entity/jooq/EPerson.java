package ua.edu.sumdu.essuir.entity.jooq;

import org.jooq.Name;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

public class EPerson extends TableImpl<Record>  {
    public static final EPerson TABLE = new EPerson();

    public final TableField<Record, Integer> epersonId = createField("eperson_id", SQLDataType.INTEGER);
    public final TableField<Record, String> firstname = createField("firstname", SQLDataType.VARCHAR(64));
    public final TableField<Record, String> lastname = createField("lastname", SQLDataType.VARCHAR(64));
    public final TableField<Record, String> email = createField("email", SQLDataType.VARCHAR);
    public final TableField<Record, Integer> chairId = createField("chair_id", SQLDataType.INTEGER);

    public EPerson() {
        super("eperson");
    }

    public EPerson(EPerson item, String alias) {
        super(alias, null, item);
    }

    @Override
    public EPerson as(String alias) {
        return new EPerson(this, alias);
    }
}
