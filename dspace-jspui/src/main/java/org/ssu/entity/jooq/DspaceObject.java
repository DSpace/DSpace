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

public class DspaceObject extends TableImpl<Record> {
    public static final DspaceObject TABLE = new DspaceObject();

    public final TableField<Record, UUID> uuid = createField("uuid", SQLDataType.UUID.identity(true));
    public final UniqueKey<Record> primaryKey = Internal.createUniqueKey(TABLE, uuid);

    public DspaceObject() {
        super("dspaceobject");
    }

    public DspaceObject(DspaceObject dspaceObject, String alias) {
        super(alias, null, dspaceObject);
    }

    @Override
    public DspaceObject as(String alias) {
        return new DspaceObject(this, alias);
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
