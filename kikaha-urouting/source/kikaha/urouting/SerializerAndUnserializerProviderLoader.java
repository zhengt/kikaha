package kikaha.urouting;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import kikaha.urouting.api.ContentType;
import kikaha.urouting.api.Serializer;
import kikaha.urouting.api.Unserializer;
import trip.spi.Producer;
import trip.spi.ProvidedServices;
import trip.spi.Singleton;

@Singleton
public class SerializerAndUnserializerProviderLoader {

	@ProvidedServices(exposedAs=Serializer.class)
	Iterable<Serializer> availableSerializers;

	@ProvidedServices(exposedAs=Unserializer.class)
	Iterable<Unserializer> availableUnserializers;
	
	SerializerAndUnserializerProvider serializerAndUnserializerProvider;

	@PostConstruct
	public void onStartup() {
		final Map<String, Serializer> serializers = loadAllSerializers();
		final Map<String, Unserializer> unserializers = loadAllUnserializers();
		serializerAndUnserializerProvider
			= new SerializerAndUnserializerProvider(serializers, unserializers);
	}
	
	@Producer
	public SerializerAndUnserializerProvider produceSerializerAndUnserializerProvider(){
		return serializerAndUnserializerProvider;
	}

	private Map<String, Serializer> loadAllSerializers() {
		final Map<String,Serializer> serializers = new HashMap<>();
		for ( final Serializer serializer : availableSerializers )
			serializers.put( extractContentTypeFrom(serializer), serializer );
		return serializers;
	}

	private Map<String, Unserializer> loadAllUnserializers() {
		final Map<String,Unserializer> unserializers = new HashMap<>();
		for ( final Unserializer unserializer : availableUnserializers )
			unserializers.put( extractContentTypeFrom(unserializer), unserializer );
		return unserializers;
	}

	String extractContentTypeFrom( final Object target ){
		final Class<?> clazz = target.getClass();
		final ContentType contentType = clazz.getAnnotation(ContentType.class);
		if ( contentType!=null )
			return contentType.value();
		throw new UnsupportedOperationException( clazz +  " must be annotated with @" + ContentType.class.getSimpleName() );
	}
}
