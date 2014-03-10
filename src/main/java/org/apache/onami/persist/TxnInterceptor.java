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

import static org.apache.onami.persist.Preconditions.checkNotNull;

/**
 * Abstract super class for all @{@link org.apache.onami.persist.Transactional} annotation interceptors.
 */
class TxnInterceptor
    implements MethodInterceptor
{

    // ---- Members

    /**
     * Unit of work.
     */
    private final UnitOfWork unitOfWork;

    /**
     * Provider for {@link TransactionFacade}.
     */
    private final TransactionFacadeProvider tfProvider;

    /**
     * Reader which extracts the {@link Transactional @Transactional} from a method invocation.
     */
    private final TransactionalAnnotationHelper txnAnnotationHelper;

    // ---- Constructor

    /**
     * Constructor.
     *
     * @param unitOfWork          the unit of work. Must not be {@code null}.
     * @param tfProvider          the provider for the {@link TransactionFacade}. Must not be {@code null}.
     * @param txnAnnotationHelper the reader for the annotations of a method. Must not be {@code null}.
     */
    public TxnInterceptor( UnitOfWork unitOfWork, TransactionFacadeProvider tfProvider,
                           TransactionalAnnotationHelper txnAnnotationHelper )
    {
        this.unitOfWork = checkNotNull( unitOfWork, "unitOfWork is mandatory!" );
        this.tfProvider = checkNotNull( tfProvider, "tfProvider is mandatory!" );
        this.txnAnnotationHelper = checkNotNull( txnAnnotationHelper, "txnAnnotationHelper is mandatory!" );
    }

    // ---- Methods

    /**
     * {@inheritDoc}
     */
    // @Override
    public final Object invoke( MethodInvocation methodInvocation )
        throws Throwable
    {
        if ( persistenceUnitParticipatesInTransactionFor( methodInvocation ) )
        {
            return invokeInTransactionAndUnitOfWork( methodInvocation );
        }
        else
        {
            return methodInvocation.proceed();
        }

    }

    private boolean persistenceUnitParticipatesInTransactionFor( MethodInvocation methodInvocation )
    {
        return txnAnnotationHelper.persistenceUnitParticipatesInTransactionFor( methodInvocation );
    }

    /**
     * Invokes the original method within a unit of work and a transaction.
     *
     * @param methodInvocation the method to be executed within the transaction
     * @return the result of the invocation of the original method.
     * @throws Throwable if an exception occurs during the call to the original method.
     */
    private Object invokeInTransactionAndUnitOfWork( MethodInvocation methodInvocation )
        throws Throwable
    {
        final boolean weStartedTheUnitOfWork = !unitOfWork.isActive();
        if ( weStartedTheUnitOfWork )
        {
            unitOfWork.begin();
        }

        Throwable originalException = null;
        try
        {
            return invokeInTransaction( methodInvocation );
        }
        catch ( Throwable exc )
        {
            originalException = exc;
            throw exc;
        }
        finally
        {
            if ( weStartedTheUnitOfWork )
            {
                endUnitOfWorkAndThrow( originalException );
            }
        }
    }

    /**
     * Ends the unit of work and throws the original exception if not null.
     *
     * @param originalException the original transaction to throw if not null.
     * @throws Throwable the original exception or an exception which occurred when closing the unit of work.
     */
    private void endUnitOfWorkAndThrow( Throwable originalException )
        throws Throwable
    {
        try
        {
            unitOfWork.end();
        }
        catch ( Throwable exc )
        {
            if ( originalException != null )
            {
                throw originalException;
            }
            else
            {
                throw exc;
            }
        }
    }

    /**
     * Invoke the original method within a transaction.
     *
     * @param methodInvocation the original method invocation.
     * @return the result of the invocation of the original method.
     * @throws Throwable if an exception occurs during the call to the original method.
     */
    private Object invokeInTransaction( MethodInvocation methodInvocation )
        throws Throwable
    {
        final TransactionFacade transactionFacade = tfProvider.getTransactionFacade();
        transactionFacade.begin();
        final Object result = invokeAndHandleException( methodInvocation, transactionFacade );
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
    private Object invokeAndHandleException( MethodInvocation methodInvocation, TransactionFacade transactionFacade )
        throws Throwable
    {
        try
        {
            return methodInvocation.proceed();
        }
        catch ( Throwable exc )
        {
            handleException( methodInvocation, transactionFacade, exc );
            throw exc;
        }
    }

    /**
     * Handles the case that an exception was thrown by the original method.
     *
     * @param methodInvocation  the original method invocation.
     * @param transactionFacade the facade to the underlying resource local or jta transaction.
     * @param exc               the exception thrown by the original method.
     */
    private void handleException( MethodInvocation methodInvocation, TransactionFacade transactionFacade,
                                  Throwable exc )
        throws Throwable
    {
        try
        {
            if ( isRollbackNecessaryFor( methodInvocation, exc ) )
            {
                transactionFacade.rollback();
            }
            else
            {
                transactionFacade.commit();
            }
        }
        catch ( Exception swallowedException )
        {
            // swallow exception from transaction facade in favor of th exception thrown by the original method.
            throw exc;
        }
    }

    private boolean isRollbackNecessaryFor( MethodInvocation methodInvocation, Throwable exc )
    {
        return txnAnnotationHelper.isRollbackNecessaryFor( methodInvocation, exc );
    }

}
