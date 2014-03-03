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

import org.aopalliance.intercept.MethodInterceptor;

import javax.persistence.EntityManager;
import javax.transaction.Status;
import java.lang.annotation.Annotation;

import static org.apache.onami.oldPersist.Preconditions.checkNotNull;

/**
 * {@link MethodInterceptor} for intercepting methods of persistence units of type JTA.
 */
class JtaTxnInterceptor
    extends AbstractTxnInterceptor
{

    // ---- Members

    /**
     * The {@link UserTransactionFacade}.
     */
    private final UserTransactionFacade utFacade;

    // ---- Constructor

    /**
     * Constructor.
     *
     * @param emProvider   the provider for {@link EntityManager}. Must not be {@code null}.
     * @param puAnntoation the annotation used for this persistence unit.
     * @param utFacade     the {@link UserTransactionFacade}. Must not be {@code null}.
     */
    public JtaTxnInterceptor( EntityManagerProviderImpl emProvider, Class<? extends Annotation> puAnntoation,
                              UserTransactionFacade utFacade )
    {
        super( emProvider, emProvider, puAnntoation );
        checkNotNull( utFacade );
        this.utFacade = utFacade;
    }

    // ---- Methods

    /**
     * {@inheritDoc}
     */
    @Override
    protected TransactionFacade getTransactionFacade( EntityManager em )
    {
        if ( Status.STATUS_NO_TRANSACTION == utFacade.getStatus() )
        {
            return new OuterTransaction( utFacade, em );
        }
        return new InnerTransaction( utFacade, em );
    }

    // ---- Inner Classes

    /**
     * TransactionFacade representing an inner (nested) transaction. Starting and
     * committing a transaction has no effect. This Facade will set the
     * rollbackOnly flag on the underlying transaction in case of a rollback.
     */
    private static class InnerTransaction
        implements TransactionFacade
    {
        private final UserTransactionFacade txn;

        private final EntityManager em;

        InnerTransaction( UserTransactionFacade txn, EntityManager em )
        {
            this.txn = txn;
            this.em = em;
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
    private static class OuterTransaction
        implements TransactionFacade
    {
        private final UserTransactionFacade txn;

        private final EntityManager em;

        OuterTransaction( UserTransactionFacade txn, EntityManager em )
        {
            this.txn = txn;
            this.em = em;
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
            if ( Status.STATUS_ACTIVE == txn.getStatus() )
            {
                txn.commit();
            }
            else
            {
                txn.rollback();
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
