package engineering.swat.nest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import engineering.swat.nest.core.bytes.ByteStream;
import engineering.swat.nest.core.bytes.source.ByteWindowBuilder;

public class CommonTestHelper {
	public static ByteStream wrap(byte... bytes) {
		try {
			return new ByteStream(ByteWindowBuilder.wrap(ByteBuffer.wrap(bytes), new URI("tmp:///test")) );
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	public static ByteStream wrap(int... bytes) {
		byte[] data = new byte[bytes.length];
		for (int i = 0; i < data.length; i++)  {
			data[i] = (byte)bytes[i];
		}
		try {
			return new ByteStream(ByteWindowBuilder.wrap(ByteBuffer.wrap(data), new URI("tmp:///test")) );
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	public static Stream<Path> findResources(String extension) throws IOException, URISyntaxException {
        ClassLoader context = Objects.requireNonNull(CommonTestHelper.class.getClassLoader(), "Unexpected missing classloader");
        URL rootDir = context.getResource("test-files/");
        if (rootDir == null) {
        	throw new IOException("Could not find /test-files/ in " + context);
        }
        List<Path> result = new ArrayList<>();
        walkURL(rootDir, new SimpleFileVisitor<Path>() {
        	@Override
        	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        		if (file.toString().endsWith(extension)) {
        			result.add(file);
        		}
        		return super.visitFile(file, attrs);
        	}
        });
        return result.stream();
	}
	
    private static void walkURL(URL rootDir, FileVisitor<Path> visitor) throws URISyntaxException, IOException {
        if (rootDir.getProtocol().equals("file")) {
            Files.walkFileTree(Paths.get(rootDir.toURI()), visitor);
        }
        else {
            try (FileSystem fs = FileSystems.newFileSystem(rootDir.toURI(), Collections.<String, Object>emptyMap())) {
                Files.walkFileTree(fs.getPath("/"), visitor);
            }
        }
    }
	 

}
