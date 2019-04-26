package engineering.swat.bird.examples.png;

import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import engineering.swat.bird.core.bytes.ByteStream;
import engineering.swat.bird.core.bytes.Context;
import engineering.swat.bird.core.bytes.source.ByteWindow;
import engineering.swat.bird.core.bytes.source.ByteWindowBuilder;
import engineering.swat.bird.example.png.PNG;

public class PNGTest {
	
	@Test
	void canParsePNG() throws IOException, URISyntaxException {
		assertNotNull(PNG.parse(buildByteSource("/test.png"), Context.DEFAULT_CONTEXT));
	}

	private ByteStream buildByteSource(String filePath) throws IOException, URISyntaxException {
		try (InputStream stream = Objects.requireNonNull(PNGTest.class.getResourceAsStream(filePath))) {
			return new ByteStream(ByteWindowBuilder.convert(stream, PNGTest.class.getResource(filePath).toURI()));
		}
	}

}
