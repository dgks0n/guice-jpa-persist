package org.apache.onami.oldPersist;

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

import static org.apache.onami.oldPersist.Preconditions.checkNotNull;

/**
 * Implementation of {@link PersistenceService} and {@link EntityManagerFactoryProvider} for
 * container managed entity manager factories.
 */
final class ContainerManagedEntityManagerFactoryProvider
    implements EntityManagerFactoryProvider, PersistenceService
{

    // ---- Members

    /**
     * The JNDI name of the {@link EntityManagerFactory}.
     */
    private final String emfJndiName;

    /**
     * The {@link EntityManagerFactory}.
     */
    private EntityManagerFactory emf;

    // ---- Constructor

    /**
     * Constructor.
     *
     * @param emfJndiName the JNDI name of the {@link EntityManagerFactory}. Must not be {@code null}.
     */
    public ContainerManagedEntityManagerFactoryProvider( String emfJndiName )
    {
        checkNotNull( emfJndiName );
        this.emfJndiName = emfJndiName;
    }

    // ---- Methods

    /**
     * {@inheritDoc}
     */
    // @Override
    public EntityManagerFactory get()
    {
        if ( isRunning() )
        {
            return emf;
        }

        throw new IllegalStateException( "PersistenceService is not running." );
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    public void start()
    {
        if ( isRunning() )
        {
            throw new IllegalStateException( "PersistenceService is already running." );
        }
        try
        {
            final InitialContext ctx = new InitialContext();
            emf = (EntityManagerFactory) ctx.lookup( emfJndiName );
        }
        catch ( NamingException e )
        {
            throw new RuntimeException( "lookup for EntityManagerFactory with JNDI name '" + emfJndiName + "' failed",
                                        e );
        }
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    public boolean isRunning()
    {
        return null != emf;
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    public void stop()
    {
        emf = null;
    }

}
