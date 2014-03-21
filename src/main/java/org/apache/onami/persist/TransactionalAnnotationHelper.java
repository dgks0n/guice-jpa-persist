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

import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.Annotation;

import static java.util.Arrays.asList;
import static org.apache.onami.persist.Preconditions.checkNotNull;

class TransactionalAnnotationHelper
{
    private final Class<? extends Annotation> puAnntoation;

    private final TransactionalAnnotationReader txnAnnoReader;

    public TransactionalAnnotationHelper( @Nullable Class<? extends Annotation> puAnntoation,
                                          TransactionalAnnotationReader txnAnnoReader )
    {
        this.puAnntoation = puAnntoation;
        this.txnAnnoReader = checkNotNull( txnAnnoReader, "txnAnnoReader is mandatory!" );
    }

    public boolean persistenceUnitParticipatesInTransactionFor( MethodInvocation methodInvocation )
    {
        return puAnntoation == null || participates( methodInvocation );
    }

    private boolean participates( MethodInvocation methodInvocation )
    {
        final Transactional transactional = txnAnnoReader.readAnnotationFrom( methodInvocation );
        final Class<? extends Annotation>[] onUnits = transactional.onUnits();
        return isEmpty( onUnits ) || contains( onUnits, puAnntoation );
    }

    private boolean isEmpty( Object[] array )
    {
        return array == null || array.length == 0;
    }

    private boolean contains( Object[] array, Object key )
    {
        return asList( array ).contains( key );
    }

    public boolean isRollbackNecessaryFor( MethodInvocation methodInvocation, Throwable e )
    {
        final Transactional transactional = txnAnnoReader.readAnnotationFrom( methodInvocation );
        return isRollbackNecessaryFor( transactional, e );
    }

    private boolean isRollbackNecessaryFor( Transactional transactional, Throwable e )
    {
        for ( Class<? extends Exception> rollbackOn : transactional.rollbackOn() )
        {
            if ( rollbackOn.isInstance( e ) )
            {
                return isNotIgnoredForRollback( transactional, e );
            }
        }
        return false;
    }

    private boolean isNotIgnoredForRollback( Transactional transactional, Throwable e )
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
