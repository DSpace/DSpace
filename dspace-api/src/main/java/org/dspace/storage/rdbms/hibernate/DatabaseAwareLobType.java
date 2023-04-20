/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.hibernate;

import org.apache.commons.lang.StringUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * A Hibernate @Type used to properly support the CLOB in both Postgres and Oracle.
 * PostgreSQL doesn't have a CLOB type, instead it's a TEXT field.
 * Normally, you'd use org.hibernate.type.TextType to support TEXT, but that won't work for Oracle.
 * https://github.com/hibernate/hibernate-orm/blob/5.6/hibernate-core/src/main/java/org/hibernate/type/TextType.java
 *
 * This Type checks if we are using PostgreSQL.
 * If so, it configures Hibernate to map CLOB to LongVarChar (same as org.hibernate.type.TextType)
 * If not, it uses default CLOB (which works for other databases).
 */
public class DatabaseAwareLobType extends AbstractSingleColumnStandardBasicType<String> {

    public static final DatabaseAwareLobType INSTANCE = new DatabaseAwareLobType();

    public DatabaseAwareLobType() {
        super( getDbDescriptor(), StringTypeDescriptor.INSTANCE );
    }

    public static SqlTypeDescriptor getDbDescriptor() {
        if ( isPostgres() ) {
            return LongVarcharTypeDescriptor.INSTANCE;
        } else {
            return ClobTypeDescriptor.DEFAULT;
        }
    }

    private static boolean isPostgres() {
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String dbDialect = configurationService.getProperty("db.dialect");

        return StringUtils.containsIgnoreCase(dbDialect, "PostgreSQL");
    }

    @Override
    public String getName() {
        return "database_aware_lob";
    }
}

