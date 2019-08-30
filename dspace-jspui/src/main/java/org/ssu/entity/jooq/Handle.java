package org.ssu.entity.jooq;

import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.util.UUID;

public class Handle extends TableImpl<Record> {
    public static final Handle TABLE = new Handle();

    public final TableField<Record, Integer> handleId = createField("handle_id", SQLDataType.INTEGER);
    public final TableField<Record, String> handle = createField("handle", SQLDataType.VARCHAR(256));
    public final TableField<Record, Integer> resourceTypeId = createField("resource_type_id", SQLDataType.INTEGER);
    public final TableField<Record, Integer> resourceLegacyId = createField("resource_legacy_id", SQLDataType.INTEGER);
    public final TableField<Record, UUID> resourceId = createField("resource_id", SQLDataType.UUID);

    public Handle() {
        super("handle");
    }

    public Handle(Handle handle, String alias) {
        super(alias, null, handle);
    }

    @Override
    public Handle as(String alias) {
        return new Handle(this, alias);
    }
}
