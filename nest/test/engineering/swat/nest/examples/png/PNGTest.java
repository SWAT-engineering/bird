package engineering.swat.nest.examples.png;

import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.source.ByteWindowBuilder;

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
