package kikaha.core.security;

import static kikaha.core.test.KikahaTestCase.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.util.HashSet;
import java.util.Set;

import kikaha.core.HttpServerExchangeStub;
import kikaha.core.impl.conf.DefaultConfiguration;
import kikaha.core.test.KikahaRunner;
import lombok.val;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith( KikahaRunner.class )
public class AuthenticationRunnerTest {

	@Mock
	SecurityContext securityContext;

	@Mock
	HttpHandler rootHandler;

	AuthenticationRunner authHandler;
	HttpServerExchange exchange;
	AuthenticationRule matchedRule;

	@Test
	public void ensureThatSetAuthenticationAsRequired() throws Exception {
		doReturn( true ).when( securityContext ).authenticate();
		when( securityContext.getAuthenticatedAccount() ).thenReturn( new FixedUsernameAndRolesAccount( createExpectedRoles(), null ) );
		authHandler.run();
		verify( securityContext ).setAuthenticationRequired();
	}

	@Test
	public void ensureThatCouldCallTheTargetHttpHandlerWhenIsAuthenticated() throws Exception {
		when( securityContext.authenticate() ).thenReturn( true );
		when( securityContext.isAuthenticated() ).thenReturn( true );
		when( securityContext.getAuthenticatedAccount() ).thenReturn( new FixedUsernameAndRolesAccount( createExpectedRoles(), null ) );
		authHandler.run();
		verify( rootHandler ).handleRequest(exchange);
	}

	@Test
	public void ensureThatNotCallTheTargetHttpHandlerWhenDoesntMatchExpectedRoles() throws Exception {
		doNothing().when( authHandler ).endCommunicationWithClient();
		doNothing().when( authHandler ).sendForbidenError();
		when( securityContext.authenticate() ).thenReturn( true );
		when( securityContext.isAuthenticated() ).thenReturn( true );
		val accountWithUnexpectedRoles = new FixedUsernameAndRolesAccount( new HashSet<String>(), null );
		when( securityContext.getAuthenticatedAccount() ).thenReturn( accountWithUnexpectedRoles );
		authHandler.run();
		verify( rootHandler, never() ).handleRequest(exchange);
		verify( authHandler ).handlePermitionDenied();
		verify( authHandler ).sendForbidenError();
	}

	@Test
	public void ensureThatNotCallTheTargetHttpHandleWhenWasNotAuthenticatedRequests() throws Exception {
		doNothing().when( authHandler ).endCommunicationWithClient();
		when( securityContext.authenticate() ).thenReturn( false );
		authHandler.run();
		verify( rootHandler, never() ).handleRequest(exchange);
		verify( authHandler ).endCommunicationWithClient();
	}

	@Test
	public void ensureThatIsAbleToHandleExceptionsInRunMethod() throws Exception {
		doThrow( new IllegalStateException() ).when( securityContext ).authenticate();
		doNothing().when( authHandler ).handleException( any( Throwable.class ) );
		authHandler.run();
		verify( rootHandler, never() ).handleRequest(exchange);
		verify( authHandler ).handleException( isA( IllegalStateException.class ) );
	}

	@Before
	public void initializeMocks() {
		MockitoAnnotations.initMocks( this );
		initializeExchange();
		initializeAuthHandler();
	}

	void initializeExchange() {
		exchange = HttpServerExchangeStub.createHttpExchange();
	}

	void initializeAuthHandler() {
		val formAuthConfig = DefaultConfiguration
			.loadDefaultConfiguration().authentication().formAuth();
		val matcher = mockAuthRuleMatcher();
		matchedRule = spy( matcher.retrieveAuthenticationRuleForUrl( "/user" ) );
		authHandler = spy( new AuthenticationRunner( exchange, rootHandler,
			securityContext, createExpectedRoles(), formAuthConfig ) );
	}

	AuthenticationRuleMatcher mockAuthRuleMatcher() {
		return spy( new AuthenticationRuleMatcher( provider, configuration.authentication() ) );
	}

	Set<String> createExpectedRoles() {
		val roles = new HashSet<String>();
		roles.add( "Expected" );
		return roles;
	}
}
