package kikaha.core;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;

import kikaha.core.api.DeploymentContext;
import kikaha.core.api.DeploymentListener;
import kikaha.core.api.KikahaException;
import kikaha.core.api.conf.Configuration;
import kikaha.core.impl.DefaultDeploymentContext;
import kikaha.core.impl.DefaultHttpRequestHandler;
import kikaha.core.impl.DefaultUndertowServerConfiguration;
import kikaha.core.rewrite.AutoHTTPSRedirectHandler;
import kikaha.core.ssl.SSLContextFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import trip.spi.DefaultServiceProvider;
import trip.spi.ServiceProvider;
import trip.spi.ServiceProviderException;

import com.typesafe.config.Config;

@Slf4j
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class UndertowServer {

	private final ServiceProvider provider;
	private final Configuration configuration;

	private DefaultUndertowServerConfiguration undertowServerConf;
	private DeploymentContext deploymentContext;
	private Undertow server;

	private String mode = "HTTP";

	public UndertowServer( final Configuration configuration ) {
		this.provider = newServiceProvider();
		this.configuration = configuration;
		provideSomeDependenciesForFurtherInjections();
		Runtime.getRuntime().addShutdownHook( new UndertowShutdownHook() );
	}

	protected void provideSomeDependenciesForFurtherInjections() {
		provider.providerFor( Configuration.class, configuration );
		provider.providerFor( Config.class, configuration.config() );
	}

	/**
	 * Start the server.
	 *
	 * @throws KikahaException
	 */
	public void start() throws KikahaException {
		val start = System.currentTimeMillis();
		bootstrap();
		this.server = createServer();
		this.server.start();
		val elapsed = System.currentTimeMillis() - start;
		reportStartupStatus(elapsed);
	}

	private void reportStartupStatus(final long elapsed) {
		log.info( "Server started in " + elapsed + "ms.");
		log.info( "Server is listening HTTP at " + configuration.host() + ":" + configuration().port() );
		if ( "HTTPS".equals( mode ) )
			log.info( "Server is also listening HTTPS at " + configuration.secureHost() + ":" + configuration().securePort() );
	}

	/**
	 * Run all life cycle initialization routines.
	 *
	 * @throws KikahaException
	 */
	protected void bootstrap() throws KikahaException {
		try {
			val deploymentContext = createDeploymentContext();
			runDeploymentHooks(deploymentContext);
			deployWebResourceFolder(deploymentContext);
			this.deploymentContext = deploymentContext;
		} catch (final ServiceProviderException cause) {
			throw new KikahaException(cause);
		}
	}

	protected DefaultDeploymentContext createDeploymentContext()
			throws ServiceProviderException {
		val deploymentHooks = provider.loadAll( DeploymentListener.class );
		return new DefaultDeploymentContext(deploymentHooks);
	}

	protected void runDeploymentHooks(final DeploymentContext deploymentContext) {
		for ( val hook : deploymentContext.deploymentHooks() ) {
			log.debug("Dispatching deployment hook: "
					+ hook.getClass().getCanonicalName());
			hook.onDeploy(deploymentContext);
		}
	}

	protected void deployWebResourceFolder(final DeploymentContext deploymentContext) {
		deploymentContext.fallbackHandler(createResourceManager());
	}

	protected ResourceHandler createResourceManager() {
		val location = retrieveWebAppFolder();
		val resourceManager = new FileResourceManager( location, 100 );
		log.info("Exposing resource files at " + location);
		return new ResourceHandler( resourceManager, new NotFoundHandler() )
				.setDirectoryListingEnabled( false )
				.setWelcomeFiles( configuration.welcomeFile() );
	}

	protected File retrieveWebAppFolder() {
		final File location = new File( configuration().resourcesPath() );
		if (!location.exists())
			location.mkdir();
		return location;
	}

	protected Undertow createServer() {
		final Builder builder = Undertow.builder().addHttpListener(
				configuration().port(), configuration().host() );
		appendSSLListenerIfConfigured(builder);
		configureServerOptions(builder);
		final HttpHandler defaultHandler = loadDefaultHttpHandler();
		return builder.setHandler( defaultHandler ).build();
	}

	HttpHandler loadDefaultHttpHandler() {
		HttpHandler defaultHandler = new DefaultHttpRequestHandler( this.deploymentContext() );
		if ( configuration().ssl().autoRedirectFromHttpToHttps() )
			defaultHandler = new AutoHTTPSRedirectHandler(defaultHandler);
		return defaultHandler;
	}

	private void configureServerOptions( final Builder builder ) {
		try {
			val serverOptionsConfiguration = provider.load(DefaultUndertowServerConfiguration.class);
			serverOptionsConfiguration.configure(builder);
		} catch (final ServiceProviderException e) {
			log.error("Can't configure the server. Shutting down...", e);
			System.exit(1);
		}
	}

	private void appendSSLListenerIfConfigured(final Builder builder) {
		val sslContext = readConfiguredSSLContext();
		if ( sslContext != null ) {
			builder.addHttpsListener( configuration().securePort(), configuration().secureHost(), sslContext );
			mode = "HTTPS";
		}
	}

	SSLContext readConfiguredSSLContext() {
		try {
			val factory = provider.load( SSLContextFactory.class );
			return factory.createSSLContext();
		} catch ( IOException | ServiceProviderException cause ) {
			throw new RuntimeException( cause );
		}
	}

	public void stop() {
		this.server.stop();
		log.info("Server stopped!");
	}

	private ServiceProvider newServiceProvider() {
		return new DefaultServiceProvider();
	}

	static <T> List<T> mutableList(final Iterable<T> immutable) {
		val mutableList = new ArrayList<T>();
		for ( val item : immutable )
			mutableList.add(item);
		return mutableList;
	}

	class UndertowShutdownHook extends Thread {

		@Override
		public void run() {
			UndertowServer.this.stop();
		}
	}
}

class NotFoundHandler implements HttpHandler {

	@Override
	public void handleRequest( HttpServerExchange exchange ) throws Exception {
		exchange.setResponseCode( 404 );
		exchange.endExchange();
	}
}