package lang.birdnescio.ide;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import org.eclipse.ui.IStartup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

public class Startup implements IStartup {

	public static final Path BIRD_HOME_FOLDER = new File(System.getProperty("user.home")).toPath().resolve(".bird");
	public static final Path NESCIO_HOME_FOLDER = new File(System.getProperty("user.home")).toPath().resolve(".nescio");
	public static final Path NESCIO_FRAMEWORK = BIRD_HOME_FOLDER.resolve("nescio.jar");
	public static final Path NEST_FRAMEWORK = BIRD_HOME_FOLDER.resolve("nest.jar");


	@Override
	public void earlyStartup() {
		ClassLoader cl = getClass().getClassLoader();
		if (cl instanceof BundleReference) {
			Bundle myBundle = ((BundleReference) cl).getBundle();
            BIRD_HOME_FOLDER.toFile().mkdirs();
			copyFile(myBundle.getResource("lib/nescio.jar"), NESCIO_FRAMEWORK);
			copyFile(myBundle.getResource("lib/nest.jar"), NEST_FRAMEWORK);
		}
	}


	private void copyFile(URL source, Path destination) {
		Path tempDestination = destination.resolveSibling(destination.getFileName() + ".tmp");
		try (InputStream from = source.openStream(); OutputStream to = Files.newOutputStream(tempDestination, StandardOpenOption.CREATE)) {
		    byte[] buffer = new byte[16 * 1024];
		    int read;
		    while ((read = from.read(buffer)) != -1) {
		        to.write(buffer, 0, read);
		    }
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		try {
			Files.move(tempDestination, destination, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			try {
				Files.move(tempDestination, destination, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
		}
	}

}
