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


import com.google.inject.Inject;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Properties;

import static org.apache.onami.persist.Preconditions.checkNotNull;

public class EntityManagerProviderImpl
    implements EntityManagerProvider, UnitOfWork
{

    // ---- Members

    /**
     * Provider for {@link javax.persistence.EntityManagerFactory}.
     */
    private final EntityManagerFactoryProvider emfProvider;

    /**
     * Additional properties to be set on every {@link EntityManager} which is created.
     */
    private final Properties properties;

    /**
     * Thread local store of {@link EntityManager}s.
     */
    private final ThreadLocal<EntityManager> entityManagers = new ThreadLocal<EntityManager>();

    // ---- Constructor

    /**
     * Constructor.
     *
     * @param emfProvider the provider for {@link javax.persistence.EntityManagerFactory}. Must not be {@code null}.
     * @param properties  additional properties to be set on every {@link EntityManager} which is created.
     */
    @Inject
    public EntityManagerProviderImpl( EntityManagerFactoryProvider emfProvider,
                                      @Nullable @ForContainerManaged Properties properties )
    {
        this.emfProvider = checkNotNull( emfProvider, "emfProvider is mandatory!" );
        this.properties = properties;
    }

    // ---- Methods


    /**
     * {@inheritDoc}
     */
    // @Override
    public EntityManager get()
        throws IllegalStateException
    {
        final EntityManager entityManager = entityManagers.get();
        if ( entityManager != null )
        {
            return entityManager;
        }
        else
        {
            throw new IllegalStateException( "UnitOfWork is not running." );
        }
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    public void begin()
    {
        if ( isActive() )
        {
            throw new IllegalStateException( "Unit of work has already been started." );
        }
        else
        {
            final EntityManager em = createEntityManager();
            entityManagers.set( em );
        }
    }

    private EntityManager createEntityManager()
    {
        final EntityManagerFactory emf = emfProvider.get();
        if ( null == properties )
        {
            return emf.createEntityManager();
        }
        else
        {
            return emf.createEntityManager( properties );
        }
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    public boolean isActive()
    {
        return entityManagers.get() != null;
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    public void end()
    {
        final EntityManager em = entityManagers.get();
        if ( em != null )
        {
            closeAndRemoveEntityManager( em );
        }
    }

    private void closeAndRemoveEntityManager( EntityManager em )
    {
        try
        {
            em.close();
        }
        finally
        {
            entityManagers.remove();
        }
    }

}
