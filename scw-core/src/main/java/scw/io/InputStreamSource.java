package scw.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple interface for objects that are sources for an {@link InputStream}.
 */
@FunctionalInterface
public interface InputStreamSource {

	/**
	 * Return an {@link InputStream} for the content of an underlying resource.
	 * <p>
	 * It is expected that each call creates a <i>fresh</i> stream.
	 * <p>
	 * This requirement is particularly important when you consider an API such as
	 * JavaMail, which needs to be able to read the stream multiple times when
	 * creating mail attachments. For such a use case, it is <i>required</i> that
	 * each {@code getInputStream()} call returns a fresh stream.
	 * 
	 * @return the input stream for the underlying resource (must not be
	 *         {@code null})
	 * @throws java.io.FileNotFoundException if the underlying resource doesn't
	 *                                       exist
	 * @throws IOException                   if the content stream could not be
	 *                                       opened
	 */
	InputStream getInputStream() throws IOException;

	/**
	 * This implementation returns {@link Channels#newChannel(InputStream)} with the
	 * result of {@link #getInputStream()}.
	 * <p>
	 * This is the same as in {@link Resource}'s corresponding default method but
	 * mirrored here for efficient JVM-level dispatching in a class hierarchy.
	 */
	default ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	default <T> T read(IoProcessor<InputStream, ? extends T> processor) throws IOException {
		InputStream is = null;
		try {
			is = getInputStream();
			return processor.process(is);
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	/**
	 * 一般不需要重写此方法，默认调用的是{@see InputStreamSource#read(IoProcessor)}
	 * 
	 * @param callback
	 * @throws IOException
	 */
	default void read(IoCallback<InputStream> callback) throws IOException {
		read((is) -> {
			callback.call(is);
			return null;
		});
	}

	default byte[] getBytes() throws IOException {
		return read((is) -> {
			return IOUtils.toByteArray(is);
		});
	}

	/**
	 * Transfer the received file to the given destination file.
	 * <p>
	 * This may either move the file in the filesystem, copy the file in the
	 * filesystem, or save memory-held contents to the destination file. If the
	 * destination file already exists, it will be deleted first.
	 * <p>
	 * If the target file has been moved in the filesystem, this operation cannot be
	 * invoked again afterwards. Therefore, call this method just once in order to
	 * work with any storage mechanism.
	 * <p>
	 * <b>NOTE:</b> Depending on the underlying provider, temporary storage may be
	 * container-dependent, including the base directory for relative destinations
	 * specified here (e.g. with Servlet 3.0 multipart handling). For absolute
	 * destinations, the target file may get renamed/moved from its temporary
	 * location or newly copied, even if a temporary copy already exists.
	 * 
	 * @param dest the destination file (typically absolute)
	 * @throws IOException           in case of reading or writing errors
	 * @throws IllegalStateException if the file has already been moved in the
	 *                               filesystem and is not available anymore for
	 *                               another transfer
	 */
	default void transferTo(File dest) throws IOException, IllegalStateException {
		read((is) -> {
			FileUtils.copyInputStreamToFile(is, dest);
		});
	}

	/**
	 * Transfer the received file to the given destination file.
	 * <p>
	 * The default implementation simply copies the file input stream.
	 * 
	 * @see #getInputStream()
	 * @see #transferTo(File)
	 */
	default void transferTo(Path dest) throws IOException, IllegalStateException {
		read((is) -> {
			FileCopyUtils.copy(is, Files.newOutputStream(dest));
		});
	}
}
