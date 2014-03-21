package org.apache.onami.persist;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Properties;

import static org.apache.onami.persist.Preconditions.checkNotNull;

/**
 * Factory for {@link EntityManagerFactory}.
 */
class EntityManagerFactoryFactory
{
    /**
     * Gets a {@link EntityManagerFactory} by looking it up in the JNDI context.
     *
     * @param jndiName jndi name of the entity manager factory. Must not be {@code null}.
     * @return the found entity manager factory
     * @throws RuntimeException when no entity manager factory was found.
     */
    EntityManagerFactory getEntityManagerFactoryByJndiLookup( String jndiName )
    {
        try
        {
            final InitialContext ctx = new InitialContext();
            final EntityManagerFactory emf = (EntityManagerFactory) ctx.lookup( jndiName );

            checkNotNull( emf, "lookup for EntityManagerFactory with JNDI name '" + jndiName + "' returned null" );

            return emf;
        }
        catch ( NamingException e )
        {
            throw new RuntimeException( "lookup for EntityManagerFactory with JNDI name '" + jndiName + "' failed", e );
        }
    }

    /**
     * Creates a new {@link EntityManagerFactory}.
     *
     * @param puName     the name of the persistence unit for which to create the entity manager factory.
     *                   Must not be {@code null}.
     * @param properties the properties to pass along when creating the entity manager factory.
     *                   Must not be {@code null}.
     * @return the newly created entity manager factory.
     */
    EntityManagerFactory createApplicationManagedEntityManagerFactory( String puName, Properties properties )
    {
        return Persistence.createEntityManagerFactory( puName, properties );
    }
}
