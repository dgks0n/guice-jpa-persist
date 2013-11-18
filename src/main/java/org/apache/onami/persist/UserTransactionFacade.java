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

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import static org.apache.onami.persist.Preconditions.checkNotNull;

/**
 * Facade to the {@link UserTransaction} which wraps all checked exception into runtime exceptions.
 */
class UserTransactionFacade
{

    // ---- Members

    private final UserTransaction txn;

    // ---- Constructor

    /**
     * Constructor.
     *
     * @param txn the actual user transaction to facade. Must not be {@code null}.
     */
    UserTransactionFacade( UserTransaction txn )
    {
        checkNotNull( txn );
        this.txn = txn;
    }

    // ---- Methods

    /**
     * @see {@link UserTransaction#begin()}.
     */
    void begin()
    {
        try
        {
            txn.begin();
        }
        catch ( NotSupportedException e )
        {
            throw new RuntimeException( "nested transactions are not supported by the user transaction " + txn, e );
        }
        catch ( SystemException e )
        {
            throw new RuntimeException( "unexpected error occurred", e );
        }
    }

    /**
     * @see {@link UserTransaction#commit()}.
     */
    void commit()
    {
        try
        {
            txn.commit();
        }
        catch ( SecurityException e )
        {
            throw new RuntimeException( "not allowed to commit the transaction", e );
        }
        catch ( IllegalStateException e )
        {
            throw new RuntimeException( "no transaction associated with userTransaction", e );
        }
        catch ( RollbackException e )
        {
            throw new RuntimeException( "rollback during commit", e );
        }
        catch ( HeuristicMixedException e )
        {
            throw new RuntimeException( "heuristic partial rollback during commit", e );
        }
        catch ( HeuristicRollbackException e )
        {
            throw new RuntimeException( "heuristic rollback during commit", e );
        }
        catch ( SystemException e )
        {
            throw new RuntimeException( "unexpected error occurred", e );
        }
    }

    /**
     * @see {@link UserTransaction#rollback()}.
     */
    void rollback()
    {
        try
        {
            txn.rollback();
        }
        catch ( IllegalStateException e )
        {
            throw new RuntimeException( "no transaction associated with userTransaction", e );
        }
        catch ( SecurityException e )
        {
            throw new RuntimeException( "not allowed to rollback the transaction", e );
        }
        catch ( SystemException e )
        {
            throw new RuntimeException( "unexpected error occurred", e );
        }
    }

    /**
     * @see {@link UserTransaction#setRollbackOnly()}.
     */
    void setRollbackOnly()
    {
        try
        {
            txn.setRollbackOnly();
        }
        catch ( IllegalStateException e )
        {
            throw new RuntimeException( "no transaction associated with userTransaction", e );
        }
        catch ( SystemException e )
        {
            throw new RuntimeException( "unexpected error occurred", e );
        }
    }

    /**
     * @see {@link UserTransaction#getStatus()}.
     */
    int getStatus()
    {
        try
        {
            int status = txn.getStatus();
            for ( int i = 0; Status.STATUS_UNKNOWN == status && i < 5; i++ )
            {
                try
                {
                    Thread.sleep( 30L );
                }
                catch ( InterruptedException e )
                {
                    // do nothing
                }
                status = txn.getStatus();
            }
            return status;
        }
        catch ( SystemException e )
        {
            throw new RuntimeException( "unexpected error occurred", e );
        }
    }
}
