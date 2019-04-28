package engineering.swat.nest.examples;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import engineering.swat.nest.CommonTestHelper;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.Context;
import engineering.swat.nest.core.bytes.source.ByteSliceBuilder;
import engineering.swat.nest.examples.formats.png.PNG;

public class PNGTest  {
	@Test
	@Ignore
	public void helpEclipse() {}

	@ParameterizedTest
	@MethodSource("pngProvider")
	public void pngFilesSucceed(Path pngFile) throws IOException, URISyntaxException {
		ByteStream stream = new ByteStream(ByteSliceBuilder.convert(Files.newInputStream(pngFile), pngFile.toUri()));
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
