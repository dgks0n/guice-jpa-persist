package org.apache.onami.persist;

import org.aopalliance.intercept.MethodInvocation;

public class TransactionalAnnotationHelper
{
    public boolean persistenceUnitParticipatesInTransactionFor( MethodInvocation methodInvocation )
    {
        throw new RuntimeException( "niy" );
    }

    public boolean isRollbackNecessaryFor( MethodInvocation methodInvocation, Throwable e )
    {
        throw new RuntimeException( "niy" );
    }
}
