package kikaha.core.rewrite;

import static org.junit.Assert.assertEquals;
import kikaha.core.api.conf.Configuration;
import kikaha.core.api.conf.RewriteRoute;
import kikaha.core.impl.conf.DefaultConfiguration;
import lombok.SneakyThrows;
import lombok.val;

import org.junit.Before;
import org.junit.Test;

import trip.spi.Provided;
import trip.spi.ServiceProvider;

public class RouteConfigurationTest {

	@Provided
	Configuration configuration;

	@Test
	public void ensureThatReadRewriteRoutesFromPredefinedTestConfigurationFile()
	{
		val routes = configuration.routes().rewriteRoutes();
		ensureReadFirstRouteAsExpected( routes.get( 0 ) );
		ensureReadSecondRouteAsExpected( routes.get( 1 ) );
		ensureReadThirdRouteAsExpected( routes.get( 2 ) );
		ensureReadFourthRouteAsExpected( routes.get( 3 ) );
	}

	private void ensureReadFirstRouteAsExpected( final RewriteRoute rewriteRoute )
	{
		assertEquals( "test.localdomain", rewriteRoute.virtualHost() );
		assertEquals( "/admin/{something}", rewriteRoute.path() );
		assertEquals( "/test/{something}/admin", rewriteRoute.target() );
	}

	private void ensureReadSecondRouteAsExpected( final RewriteRoute rewriteRoute )
	{
		assertEquals( "{virtualHost}", rewriteRoute.virtualHost() );
		assertEquals( "/admin/{something}", rewriteRoute.path() );
		assertEquals( "/test/{something}/admin", rewriteRoute.target() );
	}

	private void ensureReadThirdRouteAsExpected( final RewriteRoute rewriteRoute )
	{
		assertEquals( "test.localdomain", rewriteRoute.virtualHost() );
		assertEquals( "/{path}", rewriteRoute.path() );
		assertEquals( "/test/{something}/admin", rewriteRoute.target() );
	}

	private void ensureReadFourthRouteAsExpected( final RewriteRoute rewriteRoute )
	{
		assertEquals( "{virtualHost}", rewriteRoute.virtualHost() );
		assertEquals( "/{path}", rewriteRoute.path() );
		assertEquals( "/test/{something}/admin", rewriteRoute.target() );
	}

	@Before
	@SneakyThrows
	public void provideDependencies()
	{
		val provider = new ServiceProvider();
		provider.providerFor( Configuration.class, DefaultConfiguration.loadDefaultConfiguration() );
		provider.provideOn( this );
	}
}
