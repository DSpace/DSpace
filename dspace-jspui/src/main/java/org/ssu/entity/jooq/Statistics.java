package org.ssu.entity.jooq;

import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

public class Statistics extends TableImpl<Record> {
    public static final Statistics TABLE = new Statistics();

    public final TableField<Record, Integer> itemId = createField("item_id", SQLDataType.INTEGER);
    public final TableField<Record, Integer> sequenceId = createField("sequence_id", SQLDataType.INTEGER);
    public final TableField<Record, Integer> viewCount = createField("view_cnt", SQLDataType.INTEGER);
    public final TableField<Record, String> countryCode = createField("country_code", SQLDataType.VARCHAR(2));

    public Statistics() {
        super("statistics");
    }

    public Statistics(Statistics statistics, String alias) {
        super(alias, null, statistics);
    }

    @Override
    public Statistics as(String alias) {
        return new Statistics(this, alias);
    }
}
