package org.ssu.entity.jooq;

import org.apache.xmlbeans.impl.xb.xsdschema.Public;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.util.UUID;

public class Item extends TableImpl<Record> {
    public static final Item TABLE = new Item();

    public final TableField<Record, Integer> itemId = createField("item_id", SQLDataType.INTEGER);
    public final TableField<Record, Integer> submitterId = createField("submitter_id", SQLDataType.INTEGER);
    public final TableField<Record, Boolean> inArchive = createField("in_archive", SQLDataType.BOOLEAN);
    public final TableField<Record, UUID> uuid = createField("uuid", SQLDataType.UUID);

    public Item() {
        super("item");
    }

    public Item(Item item, String alias) {
        super(alias, null, item);
    }

    @Override
    public Item as(String alias) {
        return new Item(this, alias);
    }
}