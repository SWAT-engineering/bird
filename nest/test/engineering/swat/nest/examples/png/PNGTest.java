package engineering.swat.nest.examples.png;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import engineering.swat.nest.CommonTestHelper;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.source.ByteWindowBuilder;

public class PNGTest  {
	
	@Test
	void helpEclipse() {}
	
	@ParameterizedTest
	@MethodSource("pngProvider")
	void pngFilesSucceed(Path pngFile) throws IOException, URISyntaxException {
		ByteStream stream = new ByteStream(ByteWindowBuilder.convert(Files.newInputStream(pngFile), pngFile.toUri()));
		assertNotNull(PNG.PNG$.parse(stream, Context.DEFAULT_CONTEXT));
		assertFalse(stream.hasBytesRemaining(), "Did not consume the whole file: " + stream.getOffset() + " of " + Files.size(pngFile));
	}
	
	private static Stream<Path> pngProvider() {
		try {
			return CommonTestHelper.findResources(".png");
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	

}
