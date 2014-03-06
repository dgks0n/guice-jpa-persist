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

import javax.persistence.EntityManagerFactory;
import java.util.Properties;

import static org.apache.onami.persist.Preconditions.checkNotNull;


/**
 * Implementation of {@link org.apache.onami.oldPersist.PersistenceService} and {@link org.apache.onami.oldPersist.EntityManagerFactoryProvider} for
 * application managed persistence units.
 */
class ApplicationManagedEntityManagerFactoryProvider
    implements EntityManagerFactoryProvider, PersistenceService
{

    // ---- Members

    /**
     * Name of the persistence unit as defined in the persistence.xml.
     */
    private final String puName;

    /**
     * Additional properties. Theses override the ones defined in the persistence.xml.
     */
    private final Properties properties;

    /**
     * Factory for creating the {@link EntityManagerFactory}.
     */
    private final EntityManagerFactoryFactory emfFactory;

    /**
     * Currently active entity manager factory.
     * Is {@code null} when the persistence service is not running.
     */
    private EntityManagerFactory emf;

    // ---- Constructor

    /**
     * Constructor.
     *
     * @param puName     the name of the persistence unit as defined in the persistence.xml. Must not be {@code null}.
     * @param properties the additional properties. Theses override the ones defined in the persistence.xml. Must not be {@code null}.
     * @param emfFactory the factory for the  {@link EntityManagerFactory}. Must not be {@code null}.
     */
    @Inject
    public ApplicationManagedEntityManagerFactoryProvider( String puName, Properties properties,
                                                           EntityManagerFactoryFactory emfFactory )
    {
        this.puName = checkNotNull( puName, "puName is mandatory!" );
        this.properties = checkNotNull( properties, "properties is mandatory!" );
        this.emfFactory = checkNotNull( emfFactory, "emfFactory is mandatory!" );
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
        emf = emfFactory.createApplicationManagedEntityManagerFactory( puName, properties );
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
        if ( isRunning() )
        {
            emf.close();
            emf = null;
        }
    }

}
