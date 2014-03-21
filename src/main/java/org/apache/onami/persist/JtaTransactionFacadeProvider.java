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


import javax.persistence.EntityManager;

import static org.apache.onami.persist.Preconditions.checkNotNull;

class JtaTransactionFacadeProvider
    implements TransactionFacadeProvider
{
    private final UserTransactionFacade utFacade;

    private final EntityManagerProvider emProvider;

    public JtaTransactionFacadeProvider( UserTransactionFacade utFacade, EntityManagerProvider emProvider )
    {
        this.utFacade = checkNotNull( utFacade, "utFacade is mandatory!" );
        this.emProvider = checkNotNull( emProvider, "emProvider is mandatory!" );
    }

    // @Override
    public TransactionFacade getTransactionFacade()
    {
        if ( utFacade.isActive() )
        {
            return new Inner( utFacade, emProvider.get() );
        }
        else
        {
            return new Outer( utFacade, emProvider.get() );
        }
    }

    // ---- Inner Classes

    /**
     * TransactionFacade representing an inner (nested) transaction. Starting and
     * committing a transaction has no effect. This Facade will set the
     * rollbackOnly flag on the underlying transaction in case of a rollback.
     */
    private static class Inner
        implements TransactionFacade
    {
        private final UserTransactionFacade txn;

        private final EntityManager em;

        Inner( UserTransactionFacade txn, EntityManager em )
        {
            this.txn = checkNotNull( txn, "txn is mandatory!" );
            this.em = checkNotNull( em, "em is mandatory!" );
        }

        /**
         * {@inheritDoc}
         */
        // @Override
        public void begin()
        {
            em.joinTransaction();
        }

        /**
         * {@inheritDoc}
         */
        // @Override
        public void commit()
        {
            // Do nothing
        }

        /**
         * {@inheritDoc}
         */
        // @Override
        public void rollback()
        {
            txn.setRollbackOnly();
        }
    }

    /**
     * TransactionFacade representing an outer transaction. This Facade starts
     * and ends the transaction. If an inner transaction has set the rollbackOnly
     * flag the transaction will be rolled back in any case.
     */
    private static class Outer
        implements TransactionFacade
    {
        private final UserTransactionFacade txn;

        private final EntityManager em;

        Outer( UserTransactionFacade txn, EntityManager em )
        {
            this.txn = checkNotNull( txn, "txn is mandatory!" );
            this.em = checkNotNull( em, "em is mandatory!" );
        }

        /**
         * {@inheritDoc}
         */
        // @Override
        public void begin()
        {
            txn.begin();
            em.joinTransaction();
        }

        /**
         * {@inheritDoc}
         */
        // @Override
        public void commit()
        {
            if ( txn.getRollbackOnly() )
            {
                txn.rollback();
            }
            else
            {
                txn.commit();
            }
        }

        /**
         * {@inheritDoc}
         */
        // @Override
        public void rollback()
        {
            txn.rollback();
        }
    }

}
