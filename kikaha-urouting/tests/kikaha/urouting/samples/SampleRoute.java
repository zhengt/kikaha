package kikaha.urouting.samples;

import kikaha.urouting.User;
import kikaha.urouting.api.GET;
import kikaha.urouting.api.Mimes;
import kikaha.urouting.api.Path;
import kikaha.urouting.api.Produces;
import trip.spi.Singleton;

@Singleton
@Path( "sample" )
public class SampleRoute {

	@GET
	public String printUser() {
		return "User";
	}

	@GET
	@Path( "json" )
	@Produces( Mimes.JSON )
	public User printUserAsJSON() {
		return new User( "Handom Wser" );
	}
}
