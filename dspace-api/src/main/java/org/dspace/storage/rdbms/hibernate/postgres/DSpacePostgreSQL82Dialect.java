/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.hibernate.postgres;

import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.metamodel.spi.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.PostgresUUIDType;

/**
 * UUID's are not supported by default in hibernate due to differences in the database in order to fix this a custom sql dialect is needed.
 * Source: https://forum.hibernate.org/viewtopic.php?f=1&t=1014157
 *
 * @author kevinvandevelde at atmire.com
 */
public class DSpacePostgreSQL82Dialect extends PostgreSQL82Dialect
{
    @Override
    public void contributeTypes(final TypeContributions typeContributions, final ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        typeContributions.contributeType(new InternalPostgresUUIDType());
    }

    @Override
    protected void registerHibernateType(int code, String name) {
        super.registerHibernateType(code, name);
    }

    protected static class InternalPostgresUUIDType extends PostgresUUIDType {

        @Override
        protected boolean registerUnderJavaType() {
            return true;
        }
    }
}
