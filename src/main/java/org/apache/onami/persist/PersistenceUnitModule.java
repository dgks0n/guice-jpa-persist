package org.apache.onami.persist;

import com.google.inject.Key;
import com.google.inject.PrivateModule;

import javax.persistence.EntityManagerFactory;
import javax.transaction.UserTransaction;
import java.util.Properties;

import static org.apache.onami.persist.Preconditions.checkNotNull;

/**
 * Module for configuring a single persistence unit.
 *
 * @see PersistenceModule
 */
class PersistenceUnitModule
    extends PrivateModule
{

    /**
     * The configuration for the persistence unit.
     */
    private final PersistenceUnitModuleConfigurator config;

    /**
     * Transaction interceptor which can be passed in from the outside for injecting dependencies
     */
    private TxnInterceptor transactionInterceptor;

    /**
     * Persistence unit container which can be passed in from the outside for adding this persistence unit to it.
     */
    private PersistenceUnitContainer container;

    /**
     * Constructor.
     *
     * @param configurator the configuration holding all configs.
     */
    PersistenceUnitModule( PersistenceUnitModuleConfigurator configurator )
    {
        this.config = checkNotNull( configurator, "config is mandatory!" );
    }

    /**
     * Sets the transaction interceptor for injection of dependencies.
     *
     * @param transactionInterceptor the interceptor into which to inject dependencies.
     */
    void setTransactionInterceptor( TxnInterceptor transactionInterceptor )
    {
        this.transactionInterceptor = transactionInterceptor;
    }

    /**
     * Sets the persistence unit container for adding this persistence unit to it.
     *
     * @param container the container to which to add the persistence unit.
     */
    void setPersistenceUnitContainer( PersistenceUnitContainer container )
    {
        this.container = container;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure()
    {
        bind( Class.class ).annotatedWith( PersistenceAnnotation.class ).toInstance( config.getAnnotation() );

        bindPersistenceServiceAndEntityManagerFactoryProviderAndProperties();
        bindTransactionFacadeFactory();

        bind( EntityManagerProvider.class ).to( EntityManagerProviderImpl.class );
        bind( UnitOfWork.class ).to( EntityManagerProviderImpl.class );

        exposePersistenceServiceAndEntityManagerProviderAndUnitOfWork();

        // request injection into transaction interceptor - this adds the required dependencies to the interceptor.
        if ( transactionInterceptor != null )
        {
            requestInjection( transactionInterceptor );
        }

        // request injection into persistence unit container - this adds the current persistence unit to the container.
        if ( container != null )
        {
            requestInjection( container );
        }
    }

    /**
     * exposes the following interfaces (optionally annotated if an annotation is defined in the configuration).
     * <ul>
     * <li>{@link PersistenceService}</li>
     * <li>{@link EntityManagerProvider}</li>
     * <li>{@link UnitOfWork}</li>
     * </ul>
     */
    private void exposePersistenceServiceAndEntityManagerProviderAndUnitOfWork()
    {
        if ( config.isAnnotated() )
        {
            bindAndExposedAnnotated( PersistenceService.class );
            bindAndExposedAnnotated( EntityManagerProvider.class );
            bindAndExposedAnnotated( UnitOfWork.class );
        }
        else
        {
            expose( PersistenceService.class );
            expose( EntityManagerProvider.class );
            expose( UnitOfWork.class );
        }
    }

    /**
     * helper to expose a binding with annotation added.
     *
     * @param type the type to expose.
     * @param <T>  the type to expose.
     */
    private <T> void bindAndExposedAnnotated( Class<T> type )
    {
        bind( type ).annotatedWith( config.getAnnotation() ).to( Key.get( type ) );
        expose( type ).annotatedWith( config.getAnnotation() );
    }

    private void bindPersistenceServiceAndEntityManagerFactoryProviderAndProperties()
    {
        if ( config.isApplicationManagedPersistenceUnit() )
        {
            bindApplicationManagedPersistenceServiceAndEntityManagerFactoryProviderAndProperties();
        }
        else
        {
            bindContainerManagedPersistenceServiceAndEntityManagerFactoryProviderAndProperties();
        }
    }

    private void bindApplicationManagedPersistenceServiceAndEntityManagerFactoryProviderAndProperties()
    {
        bind( PersistenceService.class ).to( ApplicationManagedEntityManagerFactoryProvider.class );
        bind( EntityManagerFactoryProvider.class ).to( ApplicationManagedEntityManagerFactoryProvider.class );
        bind( Properties.class ).annotatedWith( ForApplicationManaged.class ).toInstance( config.getProperties() );

        // required in ApplicationManagedEntityManagerFactoryProvider
        bind( EntityManagerFactoryFactory.class );
        // required in EntityManagerFactoryFactory
        bind( String.class ).annotatedWith( ForApplicationManaged.class ).toInstance( config.getPuName() );
    }

    private void bindContainerManagedPersistenceServiceAndEntityManagerFactoryProviderAndProperties()
    {
        bind( PersistenceService.class ).to( ContainerManagedEntityManagerFactoryProvider.class );
        bind( EntityManagerFactoryProvider.class ).to( ContainerManagedEntityManagerFactoryProvider.class );
        bind( Properties.class ).annotatedWith( ForContainerManaged.class ).toInstance( config.getProperties() );

        // required in ContainerManagedEntityManagerFactoryProvider
        bindEntityManagerFactorySource();
    }

    private void bindEntityManagerFactorySource()
    {
        if ( config.isEmfProvidedByJndiLookup() )
        {
            bind( EntityManagerFactorySource.class ).to( EntityManagerFactorySourceByJndiLookup.class );

            // required in EntityManagerFactorySourceByJndiLookup
            bind( String.class ).annotatedWith( ForContainerManaged.class ).toInstance( config.getEmfJndiName() );
        }
        else
        {
            bind( EntityManagerFactorySource.class ).to( EntityManagerFactorySourceViaProvider.class );

            // required in EntityManagerFactorySourceViaProvider
            bindInternalEntityManagerFactoryProvider();
        }
    }

    private void bindInternalEntityManagerFactoryProvider()
    {
        if ( config.isEmfProvidedByInstance() )
        {
            bind( EntityManagerFactory.class ).annotatedWith( ForContainerManaged.class ).toInstance( config.getEmf() );
        }
        else if ( config.isEmfProvidedByProvider() )
        {
            bind( EntityManagerFactory.class ).annotatedWith( ForContainerManaged.class ).toProvider(
                config.getEmfProvider() );
        }
        else if ( config.isEmfProvidedByProviderKey() )
        {
            bind( EntityManagerFactory.class ).annotatedWith( ForContainerManaged.class ).toProvider(
                config.getEmfProviderKey() );
        }
        else
        {
            throw new RuntimeException( "EntityManager is improperly configured" );
        }
    }

    private void bindTransactionFacadeFactory()
    {
        if ( config.isJta() )
        {
            bindJtaTransactionFacadeFactory();
        }
        else
        {
            bind( TransactionFacadeFactory.class ).to( ResourceLocalTransactionFacadeFactory.class );
        }
    }

    private void bindJtaTransactionFacadeFactory()
    {
        bind( TransactionFacadeFactory.class ).to( JtaTransactionFacadeFactory.class );

        // required in JtaTransactionFacadeFactory
        binInternalUserTransactionProvider();
    }

    private void binInternalUserTransactionProvider()
    {
        if ( config.isUserTransactionProvidedByInstance() )
        {
            bind( UserTransaction.class ).toInstance( config.getUserTransaction() );
        }
        else if ( config.isUserTransactionProvidedByJndiLookup() )
        {
            bind( UserTransaction.class ).toProvider( UserTransactionProviderByJndiLookup.class );

            // required in UserTransactionProviderByJndiLookup
            bind( String.class ).annotatedWith( UserTransactionJndiName.class ).toInstance( config.getUtJndiName() );
        }
        else if ( config.isUserTransactionProvidedByProvider() )
        {
            bind( UserTransaction.class ).toProvider( config.getUtProvider() );
        }
        else if ( config.isUserTransactionProvidedByProviderKey() )
        {
            bind( UserTransaction.class ).toProvider( config.getUtProviderKey() );
        }
        else
        {
            throw new RuntimeException( "UserTransaction is improperly configured" );
        }
    }
}
