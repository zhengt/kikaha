package io.skullabs.undertow.urouting.json;

import io.skullabs.undertow.urouting.Mimes;

import java.io.IOException;
import java.io.Writer;

import trip.spi.Name;
import trip.spi.Service;
import urouting.api.RoutingException;
import urouting.api.Serializer;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Name( Mimes.JSON )
public class JSONSerializer implements Serializer {
	
	final ObjectMapper mapper = new ObjectMapper();

	@Override
	public <T> void serialize(T object, Writer output) throws RoutingException {
		try {
			mapper.writeValue(output, object);
		} catch (IOException cause) {
			throw new RoutingException(cause);
		}
	}

}
