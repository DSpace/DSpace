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

public class Statistics extends TableImpl<Record> {
    public static final Statistics TABLE = new Statistics();

    public final TableField<Record, Integer> sequenceId = createField("sequence_id", SQLDataType.INTEGER);
    public final TableField<Record, Integer> viewCount = createField("view_cnt", SQLDataType.INTEGER);
    public final TableField<Record, String> countryCode = createField("country_code", SQLDataType.VARCHAR(2));
    public final TableField<Record, UUID> uuid = createField("uuid",SQLDataType.UUID);

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

    @Override
    public UniqueKey<Record> getPrimaryKey() {
        return Internal.createUniqueKey(TABLE, uuid, sequenceId, countryCode);
    }

    @Override
    public List<UniqueKey<Record>> getKeys() {
        return Collections.singletonList(Internal.createUniqueKey(TABLE, uuid, sequenceId, countryCode));
    }
}
