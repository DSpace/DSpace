package org.ssu.entity.jooq;

import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.util.Collections;
import java.util.List;

public class GeneralStatistics extends TableImpl<Record> {
    public static final GeneralStatistics TABLE = new GeneralStatistics();

    public final TableField<Record, Integer> year = createField("year", SQLDataType.INTEGER.identity(true));
    public final TableField<Record, Integer> month = createField("month", SQLDataType.INTEGER.identity(true));
    public final TableField<Record, Integer> viewCount = createField("count_views", SQLDataType.INTEGER);
    public final TableField<Record, Integer> downloadsCount = createField("count_downloads", SQLDataType.INTEGER);
    public final UniqueKey<Record> primaryKey = Internal.createUniqueKey(TABLE, year, month);

    public GeneralStatistics() {
        super("general_statistics");
    }

    public GeneralStatistics(GeneralStatistics statistics, String alias) {
        super(alias, null, statistics);
    }

    @Override
    public GeneralStatistics as(String alias) {
        return new GeneralStatistics(this, alias);
    }

    @Override
    public UniqueKey<Record> getPrimaryKey() {
        return Internal.createUniqueKey(TABLE, year, month);
    }

    @Override
    public List<UniqueKey<Record>> getKeys() {
        return Collections.singletonList(Internal.createUniqueKey(TABLE, year, month));
    }

}
