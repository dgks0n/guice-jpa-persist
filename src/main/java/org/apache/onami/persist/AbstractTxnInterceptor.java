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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import javax.persistence.EntityManager;
import java.lang.annotation.Annotation;

import static org.apache.onami.persist.Preconditions.checkNotNull;

/**
 * Abstract super class for all @{@link Transactional} annotation interceptors.
 */
abstract class AbstractTxnInterceptor
    implements MethodInterceptor
{

    // ---- Members

    /**
     * Unit of work.
     */
    private final UnitOfWork unitOfWork;

    /**
     * Provider for {@link EntityManager}.
     */
    private final EntityManagerProvider emProvider;

    /**
     * Annotation of the persistence unit this interceptor belongs to.
     */
    private final Class<? extends Annotation> puAnntoation;

    private final TransactionalAnnotationReader transactionalAnnotationReader = new TransactionalAnnotationReader();

    // ---- Constructor

    /**
     * Constructor.
     *
     * @param emProvider   the provider for {@link EntityManager}. Must not be {@code null}.
     * @param unitOfWork   the unit of work. Must not be {@code null}.
     * @param puAnntoation the annotation of the persistence unit the interceptor belongs to.
     *                     May be {@code null}.
     */
    public AbstractTxnInterceptor( EntityManagerProvider emProvider, UnitOfWork unitOfWork,
                                   Class<? extends Annotation> puAnntoation )
    {
        checkNotNull( unitOfWork );
        checkNotNull( emProvider );

        this.unitOfWork = unitOfWork;
        this.emProvider = emProvider;
        this.puAnntoation = puAnntoation;
    }

    // ---- Methods

    /**
     * {@inheritDoc}
     */
    @Override
    public final Object invoke( MethodInvocation methodInvocation )
        throws Throwable
    {

        if ( !transactionCoversThisPersistenceUnit( methodInvocation ) )
        {
            return methodInvocation.proceed();
        }

        final boolean weStartedTheUnitOfWork = !unitOfWork.isActive();
        if ( weStartedTheUnitOfWork )
        {
            unitOfWork.begin();
        }

        try
        {
            final EntityManager em = emProvider.get();
            final TransactionFacade transactionFacade = getTransactionFacade( em );

            return invoke( methodInvocation, transactionFacade );
        }
        finally
        {
            if ( weStartedTheUnitOfWork )
            {
                unitOfWork.end();
            }
        }
    }

    /**
     * Check whether the persistence unit of this interceptor participates in the transaction or not.
     *
     * @param methodInvocation the original method invocation.
     * @return {@code true} if the persistence unit participates in the transaction
     *         {@code false} otherwise.
     */
    private boolean transactionCoversThisPersistenceUnit( MethodInvocation methodInvocation )
    {
        if ( null == puAnntoation )
        {
            return true;
        }

        final Transactional localTransaction = transactionalAnnotationReader.readAnnotationFrom( methodInvocation );
        final Class<? extends Annotation>[] units = localTransaction.onUnits();
        if ( null == units || 0 == units.length )
        {
            return true;
        }

        final int n = units.length;
        for ( int i = 0; i < n; i++ )
        {
            if ( puAnntoation.equals( units[i] ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the transaction facade for the given entity manager.
     *
     * @param em the entity manager.
     * @return the transaction facade. Never {@code null}.
     */
    protected abstract TransactionFacade getTransactionFacade( final EntityManager em );

    /**
     * Invoke the original method surrounded by a transaction.
     *
     * @param methodInvocation  the original method invocation.
     * @param transactionFacade the facade to the underling resource local or jta transaction.
     * @return the result of the invocation of the original method.
     * @throws Throwable if an exception occurs during the call to the original method.
     */
    private Object invoke( MethodInvocation methodInvocation, TransactionFacade transactionFacade )
        throws Throwable
    {

        transactionFacade.begin();
        final Object result = doTransactional( methodInvocation, transactionFacade );
        transactionFacade.commit();

        return result;
    }

    /**
     * Invoke the original method assuming a transaction has already been started.
     * This method is responsible of calling rollback if necessary.
     *
     * @param methodInvocation  the original method invocation.
     * @param transactionFacade the facade to the underlying resource local or jta transaction.
     * @return the result of the invocation of the original method.
     * @throws Throwable if an exception occurs during the call to the original method.
     */
    private Object doTransactional( MethodInvocation methodInvocation, TransactionFacade transactionFacade )
        throws Throwable
    {
        try
        {
            return methodInvocation.proceed();
        }
        catch ( Throwable e )
        {
            final Transactional t = transactionalAnnotationReader.readAnnotationFrom( methodInvocation );
            if ( rollbackIsNecessary( t, e ) )
            {
                transactionFacade.rollback();
            }
            else
            {
                transactionFacade.commit();
            }
            // In any case: throw the original exception.
            throw e;
        }
    }

    /**
     * Returns True if a rollback is necessary.
     *
     * @param transactional The metadata annotation of the method.
     * @param e             The exception to test for rollback.
     * @return {@code true} if a rollback is necessary, {@code false} otherwise.
     */
    private boolean rollbackIsNecessary( Transactional transactional, Throwable e )
    {
        for ( Class<? extends Exception> rollbackOn : transactional.rollbackOn() )
        {
            if ( rollbackOn.isInstance( e ) )
            {
                for ( Class<? extends Exception> ignore : transactional.ignore() )
                {
                    if ( ignore.isInstance( e ) )
                    {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

}
