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

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Container of persistence units. This is a convenience wrapper for multiple
 * persistence units. calling any method of either {@link PersistenceService} or
 * {@link UnitOfWork} will propagate this call to all added persistence units.
 */
class PersistenceUnitContainer
    implements PersistenceService, UnitOfWork
{

    // ---- Members

    /**
     * Collection of all known persistence services.
     */
    private final Set<PersistenceService> persistenceServices = new HashSet<PersistenceService>();

    /**
     * Collection of all known units of work.
     */
    private final Set<UnitOfWork> unitsOfWork = new HashSet<UnitOfWork>();

    // ---- Methods

    /**
     * Adds a persistence service and a unit of work to this container.
     *
     * @param ps  the persistence service to add. Must not be {@code null}.
     * @param uow the unit of work to add. Must not be {@code null}.
     */
    void add( PersistenceService ps, UnitOfWork uow )
    {
        checkNotNull( ps );
        checkNotNull( uow );
        persistenceServices.add( ps );
        unitsOfWork.add( uow );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start()
    {
        for ( PersistenceService ps : persistenceServices )
        {
            ps.start();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isRunning()
    {
        for ( PersistenceService ps : persistenceServices )
        {
            if ( !ps.isRunning() )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stop()
    {
        for ( PersistenceService ps : persistenceServices )
        {
            ps.stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void begin()
    {
        for ( UnitOfWork unitOfWork : unitsOfWork )
        {
            unitOfWork.begin();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive()
    {
        for ( UnitOfWork unitOfWork : unitsOfWork )
        {
            if ( !unitOfWork.isActive() )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void end()
    {
        for ( UnitOfWork unitOfWork : unitsOfWork )
        {
            unitOfWork.end();
        }

    }

}
