package scw.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import scw.core.utils.ArrayUtils;

public final class IOUtils {
	// NOTE: This class is focussed on InputStream, OutputStream, Reader and
	// Writer. Each method should take at least one of these as a parameter,
	// or return one of them.

	private static final int EOF = -1;
	/**
	 * The Unix directory separator character.
	 */
	public static final char DIR_SEPARATOR_UNIX = '/';
	/**
	 * The Windows directory separator character.
	 */
	public static final char DIR_SEPARATOR_WINDOWS = '\\';
	/**
	 * The system directory separator character.
	 */
	public static final char DIR_SEPARATOR = File.separatorChar;
	/**
	 * The Unix line separator string.
	 */
	public static final String LINE_SEPARATOR_UNIX = "\n";
	/**
	 * The Windows line separator string.
	 */
	public static final String LINE_SEPARATOR_WINDOWS = "\r\n";
	/**
	 * The system line separator string.
	 */
	public static final String LINE_SEPARATOR;

	static {
		// avoid security issues
		UnsafeStringWriter buf = new UnsafeStringWriter(4);
		PrintWriter out = new PrintWriter(buf);
		out.println();
		LINE_SEPARATOR = buf.toString();
		out.close();
	}

	/**
	 * The default buffer size ({@value} ) to use for
	 * {@link #copyLarge(InputStream, OutputStream)} and
	 * {@link #copyLarge(Reader, Writer)}
	 */
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	/**
	 * The default buffer size to use for the skip() methods.
	 */
	private static final int SKIP_BUFFER_SIZE = 2048;

	// Allocated in the relevant skip method if necessary.
	/*
	 * N.B. no need to synchronize these because: - we don't care if the buffer
	 * is created multiple times (the data is ignored) - we always use the same
	 * size buffer, so if it it is recreated it will still be OK (if the buffer
	 * size were variable, we would need to synch. to ensure some other thread
	 * did not create a smaller one)
	 */
	private static char[] SKIP_CHAR_BUFFER;
	private static byte[] SKIP_BYTE_BUFFER;

	/**
	 * Instances should NOT be constructed in standard programming.
	 */
	private IOUtils() {
	}

	public static InputStream limitedInputStream(final InputStream is, final int limit) throws IOException {
		return new InputStream() {
			private int mPosition = 0, mMark = 0, mLimit = Math.min(limit, is.available());

			public int read() throws IOException {
				if (mPosition < mLimit) {
					mPosition++;
					return is.read();
				}
				return -1;
			}

			public int read(byte b[], int off, int len) throws IOException {
				if (b == null)
					throw new NullPointerException();

				if (off < 0 || len < 0 || len > b.length - off)
					throw new IndexOutOfBoundsException();

				if (mPosition >= mLimit)
					return -1;

				if (mPosition + len > mLimit)
					len = mLimit - mPosition;

				if (len <= 0)
					return 0;

				is.read(b, off, len);
				mPosition += len;
				return len;
			}

			public long skip(long len) throws IOException {
				if (mPosition + len > mLimit)
					len = mLimit - mPosition;

				if (len <= 0)
					return 0;

				is.skip(len);
				mPosition += len;
				return len;
			}

			public int available() {
				return mLimit - mPosition;
			}

			public boolean markSupported() {
				return is.markSupported();
			}

			public void mark(int readlimit) {
				is.mark(readlimit);
				mMark = mPosition;
			}

			public void reset() throws IOException {
				is.reset();
				mPosition = mMark;
			}

			public void close() throws IOException {
			}
		};
	}

	public static InputStream markSupportedInputStream(final InputStream is, final int markBufferSize) {
		if (is.markSupported()) {
			return is;
		}

		return new InputStream() {
			byte[] mMarkBuffer;

			boolean mInMarked = false;
			boolean mInReset = false;
			private int mPosition = 0;
			private int mCount = 0;

			boolean mDry = false;

			@Override
			public int read() throws IOException {
				if (!mInMarked) {
					return is.read();
				} else {
					if (mPosition < mCount) {
						byte b = mMarkBuffer[mPosition++];
						return b & 0xFF;
					}

					if (!mInReset) {
						if (mDry)
							return -1;

						if (null == mMarkBuffer) {
							mMarkBuffer = new byte[markBufferSize];
						}
						if (mPosition >= markBufferSize) {
							throw new IOException("Mark buffer is full!");
						}

						int read = is.read();
						if (-1 == read) {
							mDry = true;
							return -1;
						}

						mMarkBuffer[mPosition++] = (byte) read;
						mCount++;

						return read;
					} else {
						// mark buffer is used, exit mark status!
						mInMarked = false;
						mInReset = false;
						mPosition = 0;
						mCount = 0;

						return is.read();
					}
				}
			}

			/**
			 * NOTE: the <code>readlimit</code> argument for this class has no
			 * meaning.
			 */
			@Override
			public synchronized void mark(int readlimit) {
				mInMarked = true;
				mInReset = false;

				// mark buffer is not empty
				int count = mCount - mPosition;
				if (count > 0) {
					System.arraycopy(mMarkBuffer, mPosition, mMarkBuffer, 0, count);
					mCount = count;
					mPosition = 0;
				}
			}

			@Override
			public synchronized void reset() throws IOException {
				if (!mInMarked) {
					throw new IOException("should mark befor reset!");
				}

				mInReset = true;
				mPosition = 0;
			}

			@Override
			public boolean markSupported() {
				return true;
			}

			@Override
			public int available() throws IOException {
				int available = is.available();

				if (mInMarked && mInReset)
					available += mCount - mPosition;

				return available;
			}
		};
	}

	public static InputStream markSupportedInputStream(final InputStream is) {
		return markSupportedInputStream(is, 1024);
	}

	public static void skipUnusedStream(InputStream is) throws IOException {
		if (is.available() > 0) {
			is.skip(is.available());
		}
	}

	public static long write(OutputStream os, InputStream is, int buffSize, int mark) throws IOException {
		if (mark >= 0) {
			if (is.markSupported()) {
				is.mark(0);
			}
		}

		try {
			return write(is, os, buffSize);
		} finally {
			if (mark >= 0) {
				if (is.markSupported()) {
					is.reset();
				}
			}
		}
	}

	/**
	 * write.
	 * 
	 * @param is
	 *            InputStream instance
	 * @param os
	 *            OutputStream instance
	 * @return count
	 * @throws IOException
	 */
	public static long write(InputStream is, OutputStream os) throws IOException {
		return write(is, os, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * write.
	 * 
	 * @param is
	 *            InputStream instance
	 * @param os
	 *            OutputStream instance
	 * @param bufferSize
	 *            buffer size
	 * @return count
	 * @throws IOException
	 */
	public static long write(InputStream is, OutputStream os, int bufferSize) throws IOException {
		int read;
		long total = 0;
		byte[] buff = new byte[bufferSize];
		while (is.available() > 0) {
			read = is.read(buff, 0, buff.length);
			if (read > 0) {
				os.write(buff, 0, read);
				total += read;
			}
		}
		return total;
	}

	/**
	 * write lines.
	 * 
	 * @param os
	 *            output stream.
	 * @param lines
	 *            lines.
	 * @throws IOException
	 */
	public static void writeLines(OutputStream os, String[] lines) throws IOException {
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(os));
		try {
			for (String line : lines) {
				writer.println(line);
			}
			writer.flush();
		} finally {
			writer.close();
		}
	}

	/**
	 * write lines.
	 * 
	 * @param file
	 *            file.
	 * @param lines
	 *            lines.
	 * @throws IOException
	 */
	public static void writeLines(File file, String[] lines) throws IOException {
		if (file == null) {
			throw new IOException("File is null.");
		}
		writeLines(new FileOutputStream(file), lines);
	}

	/**
	 * append lines.
	 * 
	 * @param file
	 *            file.
	 * @param lines
	 *            lines.
	 * @throws IOException
	 */
	public static void appendLines(File file, String[] lines) throws IOException {
		if (file == null) {
			throw new IOException("File is null.");
		}
		writeLines(new FileOutputStream(file, true), lines);
	}

	/**
	 * 读取内容
	 * 
	 * @param in
	 * @param buffSize
	 * @param mark
	 *            标记的位置 如果小于0就不标记
	 * @return
	 * @throws IOException
	 */
	public static ByteArray read(InputStream in, int buffSize, int mark) throws IOException {
		ByteArray byteArray = new ByteArray(buffSize);
		byte[] b = new byte[buffSize];
		int len;
		try {
			if (mark >= 0) {
				if (in.markSupported()) {
					in.mark(0);
				}
			}

			while ((len = in.read(b)) != -1) {
				byteArray.write(b, 0, len);
			}
			return byteArray;
		} finally {
			if (mark >= 0) {
				if (in.markSupported()) {
					in.reset();
				}
			}
		}
	}

	public static String read(Reader reader, int buffSize, int mark) throws IOException {
		StringBuilder sb = new StringBuilder(buffSize);
		char[] buff = new char[buffSize];
		int len;
		try {
			if (mark >= 0) {
				if (reader.markSupported()) {
					reader.mark(0);
				}
			}

			while ((len = reader.read(buff)) != -1) {
				sb.append(buff, 0, len);
			}
			return sb.toString();
		} finally {
			if (mark >= 0) {
				if (reader.markSupported()) {
					reader.reset();
				}
			}
		}
	}

	public static String read(BufferedReader br, int mark) throws IOException {
		StringBuilder sb = new StringBuilder();
		String line;
		try {
			if (mark >= 0) {
				if (br.markSupported()) {
					br.mark(0);
				}
			}

			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			return sb.toString();
		} finally {
			if (mark >= 0) {
				if (br.markSupported()) {
					br.reset();
				}
			}
		}
	}

	public static List<String> readLineList(BufferedReader br, int mark) throws IOException {
		List<String> list = new ArrayList<String>();
		String line;
		try {
			if (mark >= 0) {
				if (br.markSupported()) {
					br.mark(0);
				}
			}

			while ((line = br.readLine()) != null) {
				list.add(line);
			}
			return list;
		} finally {
			if (mark >= 0) {
				if (br.markSupported()) {
					br.reset();
				}
			}
		}
	}

	public static void close(boolean printStackTrace, Closeable... closeables) {
		if (ArrayUtils.isEmpty(closeables)) {
			return;
		}

		for (Closeable closeable : closeables) {
			close(printStackTrace, closeable);
		}
	}

	public static void close(boolean printStackTrace, Closeable closeable) {
		if (closeable == null) {
			return;
		}

		try {
			closeable.close();
		} catch (IOException e) {
			if (printStackTrace) {
				e.printStackTrace();
			}
		}
	}

	public static void close(Closeable closeable) {
		close(true, closeable);
	}

	public static void close(Closeable... closeables) {
		close(true, closeables);
	}

	/**
	 * Unconditionally close a <code>Writer</code>.
	 * <p>
	 * Equivalent to {@link Writer#close()}, except any exceptions will be
	 * ignored. This is typically used in finally blocks.
	 * <p>
	 * Example code:
	 * 
	 * <pre>
	 * Writer out = null;
	 * try {
	 * 	out = new StringWriter();
	 * 	out.write(&quot;Hello World&quot;);
	 * 	out.close(); // close errors are handled
	 * } catch (Exception e) {
	 * 	// error handling
	 * } finally {
	 * 	IOUtils.closeQuietly(out);
	 * }
	 * </pre>
	 *
	 * @param output
	 *            the Writer to close, may be null or already closed
	 */
	public static void closeQuietly(Closeable closeable) {
		close(false, closeable);
	}

	/**
	 * Returns the given reader if it is a {@link BufferedReader}, otherwise
	 * creates a toBufferedReader for the given reader.
	 * 
	 * @param reader
	 *            the reader to wrap or return
	 * @return the given reader or a new {@link BufferedReader} for the given
	 *         reader
	 * @since 2.2
	 */
	public static BufferedReader toBufferedReader(Reader reader) {
		return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
	}

	// read toByteArray
	// -----------------------------------------------------------------------
	/**
	 * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @return the requested byte array
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static byte[] toByteArray(InputStream input) throws IOException {
		UnsafeByteArrayOutputStream output = new UnsafeByteArrayOutputStream();	
		copy(input, output);
		return output.toByteArray();
	}

	/**
	 * Get contents of an <code>InputStream</code> as a <code>byte[]</code>. Use
	 * this method instead of <code>toByteArray(InputStream)</code> when
	 * <code>InputStream</code> size is known. <b>NOTE:</b> the method checks
	 * that the length can safely be cast to an int without truncation before
	 * using {@link IOUtils#toByteArray(java.io.InputStream, int)} to read into
	 * the byte array. (Arrays can have no more than Integer.MAX_VALUE entries
	 * anyway)
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param size
	 *            the size of <code>InputStream</code>
	 * @return the requested byte array
	 * @throws IOException
	 *             if an I/O error occurs or <code>InputStream</code> size
	 *             differ from parameter size
	 * @throws IllegalArgumentException
	 *             if size is less than zero or size is greater than
	 *             Integer.MAX_VALUE
	 * @see IOUtils#toByteArray(java.io.InputStream, int)
	 * @since 2.1
	 */
	public static byte[] toByteArray(InputStream input, long size) throws IOException {

		if (size > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Size cannot be greater than Integer max value: " + size);
		}

		return toByteArray(input, (int) size);
	}

	/**
	 * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
	 * Use this method instead of <code>toByteArray(InputStream)</code> when
	 * <code>InputStream</code> size is known
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param size
	 *            the size of <code>InputStream</code>
	 * @return the requested byte array
	 * @throws IOException
	 *             if an I/O error occurs or <code>InputStream</code> size
	 *             differ from parameter size
	 * @throws IllegalArgumentException
	 *             if size is less than zero
	 * @since 2.1
	 */
	public static byte[] toByteArray(InputStream input, int size) throws IOException {

		if (size < 0) {
			throw new IllegalArgumentException("Size must be equal or greater than zero: " + size);
		}

		if (size == 0) {
			return new byte[0];
		}

		byte[] data = new byte[size];
		int offset = 0;
		int readed;

		while (offset < size && (readed = input.read(data, offset, size - offset)) != EOF) {
			offset += readed;
		}

		if (offset != size) {
			throw new IOException("Unexpected readed size. current: " + offset + ", excepted: " + size);
		}

		return data;
	}

	/**
	 * Get the contents of a <code>Reader</code> as a <code>byte[]</code> using
	 * the default character encoding of the platform.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedReader</code>.
	 * 
	 * @param input
	 *            the <code>Reader</code> to read from
	 * @return the requested byte array
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static byte[] toByteArray(Reader input) throws IOException {
		UnsafeByteArrayOutputStream output = new UnsafeByteArrayOutputStream();
		copy(input, output);
		return output.toByteArray();
	}

	/**
	 * Get the contents of a <code>Reader</code> as a <code>byte[]</code> using
	 * the specified character encoding.
	 * <p>
	 * Character encoding names can be found at
	 * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedReader</code>.
	 * 
	 * @param input
	 *            the <code>Reader</code> to read from
	 * @param encoding
	 *            the encoding to use, null means platform default
	 * @return the requested byte array
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static byte[] toByteArray(Reader input, String encoding) throws IOException {
		UnsafeByteArrayOutputStream output = new UnsafeByteArrayOutputStream();
		copy(input, output, encoding);
		return output.toByteArray();
	}

	// read char[]
	// -----------------------------------------------------------------------
	/**
	 * Get the contents of an <code>InputStream</code> as a character array
	 * using the default character encoding of the platform.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * 
	 * @param is
	 *            the <code>InputStream</code> to read from
	 * @return the requested character array
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static char[] toCharArray(InputStream is) throws IOException {
		CharArrayWriter output = new CharArrayWriter();
		copy(is, output);
		return output.toCharArray();
	}

	/**
	 * Get the contents of an <code>InputStream</code> as a character array
	 * using the specified character encoding.
	 * <p>
	 * Character encoding names can be found at
	 * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * 
	 * @param is
	 *            the <code>InputStream</code> to read from
	 * @param encoding
	 *            the encoding to use, null means platform default
	 * @return the requested character array
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static char[] toCharArray(InputStream is, String encoding) throws IOException {
		CharArrayWriter output = new CharArrayWriter();
		copy(is, output, encoding);
		return output.toCharArray();
	}

	/**
	 * Get the contents of a <code>Reader</code> as a character array.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedReader</code>.
	 * 
	 * @param input
	 *            the <code>Reader</code> to read from
	 * @return the requested character array
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static char[] toCharArray(Reader input) throws IOException {
		CharArrayWriter sw = new CharArrayWriter();
		copy(input, sw);
		return sw.toCharArray();
	}

	// read toString
	// -----------------------------------------------------------------------
	/**
	 * Get the contents of an <code>InputStream</code> as a String using the
	 * default character encoding of the platform.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @return the requested String
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static String toString(InputStream input) throws IOException {
		return toString(input, null);
	}

	/**
	 * Get the contents of an <code>InputStream</code> as a String using the
	 * specified character encoding.
	 * <p>
	 * Character encoding names can be found at
	 * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param encoding
	 *            the encoding to use, null means platform default
	 * @return the requested String
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static String toString(InputStream input, String encoding) throws IOException {
		UnsafeStringWriter sw = new UnsafeStringWriter();
		copy(input, sw, encoding);
		return sw.toString();
	}

	/**
	 * Get the contents of a <code>Reader</code> as a String.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedReader</code>.
	 * 
	 * @param input
	 *            the <code>Reader</code> to read from
	 * @return the requested String
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static String toString(Reader input) throws IOException {
		UnsafeStringWriter sw = new UnsafeStringWriter();
		copy(input, sw);
		return sw.toString();
	}

	/**
	 * Gets the contents at the given URI.
	 * 
	 * @param uri
	 *            The URI source.
	 * @return The contents of the URL as a String.
	 * @throws IOException
	 *             if an I/O exception occurs.
	 * @since 2.1.
	 */
	public static String toString(URI uri) throws IOException {
		return toString(uri, null);
	}

	/**
	 * Gets the contents at the given URI.
	 * 
	 * @param uri
	 *            The URI source.
	 * @param encoding
	 *            The encoding name for the URL contents.
	 * @return The contents of the URL as a String.
	 * @throws IOException
	 *             if an I/O exception occurs.
	 * @since 2.1.
	 */
	public static String toString(URI uri, String encoding) throws IOException {
		return toString(uri.toURL(), encoding);
	}

	/**
	 * Gets the contents at the given URL.
	 * 
	 * @param url
	 *            The URL source.
	 * @return The contents of the URL as a String.
	 * @throws IOException
	 *             if an I/O exception occurs.
	 * @since 2.1.
	 */
	public static String toString(URL url) throws IOException {
		return toString(url, null);
	}

	/**
	 * Gets the contents at the given URL.
	 * 
	 * @param url
	 *            The URL source.
	 * @param encoding
	 *            The encoding name for the URL contents.
	 * @return The contents of the URL as a String.
	 * @throws IOException
	 *             if an I/O exception occurs.
	 * @since 2.1.
	 */
	public static String toString(URL url, String encoding) throws IOException {
		InputStream inputStream = url.openStream();
		try {
			return toString(inputStream, encoding);
		} finally {
			inputStream.close();
		}
	}

	// readLines
	// -----------------------------------------------------------------------
	/**
	 * Get the contents of an <code>InputStream</code> as a list of Strings, one
	 * entry per line, using the default character encoding of the platform.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 *
	 * @param input
	 *            the <code>InputStream</code> to read from, not null
	 * @return the list of Strings, never null
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static List<String> readLines(InputStream input) throws IOException {
		InputStreamReader reader = new InputStreamReader(input);
		return readLines(reader);
	}

	/**
	 * Get the contents of an <code>InputStream</code> as a list of Strings, one
	 * entry per line, using the specified character encoding.
	 * <p>
	 * Character encoding names can be found at
	 * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 *
	 * @param input
	 *            the <code>InputStream</code> to read from, not null
	 * @param encoding
	 *            the encoding to use, null means platform default
	 * @return the list of Strings, never null
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static List<String> readLines(InputStream input, String encoding) throws IOException {
		if (encoding == null) {
			return readLines(input);
		} else {
			InputStreamReader reader = new InputStreamReader(input, encoding);
			return readLines(reader);
		}
	}

	/**
	 * Get the contents of a <code>Reader</code> as a list of Strings, one entry
	 * per line.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedReader</code>.
	 *
	 * @param input
	 *            the <code>Reader</code> to read from, not null
	 * @return the list of Strings, never null
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static List<String> readLines(Reader input) throws IOException {
		BufferedReader reader = toBufferedReader(input);
		List<String> list = new ArrayList<String>();
		String line = reader.readLine();
		while (line != null) {
			list.add(line);
			line = reader.readLine();
		}
		return list;
	}

	// -----------------------------------------------------------------------
	/**
	 * Convert the specified CharSequence to an input stream, encoded as bytes
	 * using the default character encoding of the platform.
	 *
	 * @param input
	 *            the CharSequence to convert
	 * @return an input stream
	 * @since 2.0
	 */
	public static InputStream toInputStream(CharSequence input) {
		return toInputStream(input.toString());
	}

	/**
	 * Convert the specified CharSequence to an input stream, encoded as bytes
	 * using the specified character encoding.
	 * <p>
	 * Character encoding names can be found at
	 * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
	 *
	 * @param input
	 *            the CharSequence to convert
	 * @param encoding
	 *            the encoding to use, null means platform default
	 * @throws IOException
	 *             if the encoding is invalid
	 * @return an input stream
	 * @since 2.0
	 */
	public static InputStream toInputStream(CharSequence input, String encoding) throws IOException {
		return toInputStream(input.toString(), encoding);
	}

	// -----------------------------------------------------------------------
	/**
	 * Convert the specified string to an input stream, encoded as bytes using
	 * the default character encoding of the platform.
	 *
	 * @param input
	 *            the string to convert
	 * @return an input stream
	 * @since 1.1
	 */
	public static InputStream toInputStream(String input) {
		byte[] bytes = input.getBytes();
		return new ByteArrayInputStream(bytes);
	}

	/**
	 * Convert the specified string to an input stream, encoded as bytes using
	 * the specified character encoding.
	 * <p>
	 * Character encoding names can be found at
	 * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
	 *
	 * @param input
	 *            the string to convert
	 * @param encoding
	 *            the encoding to use, null means platform default
	 * @throws IOException
	 *             if the encoding is invalid
	 * @return an input stream
	 * @since 1.1
	 */
	public static InputStream toInputStream(String input, String encoding) throws IOException {
		byte[] bytes = encoding != null ? input.getBytes(encoding) : input.getBytes();
		return new ByteArrayInputStream(bytes);
	}

	// write byte[]
	// -----------------------------------------------------------------------
	/**
	 * Writes bytes from a <code>byte[]</code> to an <code>OutputStream</code>.
	 * 
	 * @param data
	 *            the byte array to write, do not modify during output, null
	 *            ignored
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @throws NullPointerException
	 *             if output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void write(byte[] data, OutputStream output) throws IOException {
		if (data != null) {
			output.write(data);
		}
	}

	/**
	 * Writes bytes from a <code>byte[]</code> to chars on a <code>Writer</code>
	 * using the default character encoding of the platform.
	 * <p>
	 * This method uses {@link String#String(byte[])}.
	 * 
	 * @param data
	 *            the byte array to write, do not modify during output, null
	 *            ignored
	 * @param output
	 *            the <code>Writer</code> to write to
	 * @throws NullPointerException
	 *             if output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void write(byte[] data, Writer output) throws IOException {
		if (data != null) {
			output.write(new String(data));
		}
	}

	/**
	 * Writes bytes from a <code>byte[]</code> to chars on a <code>Writer</code>
	 * using the specified character encoding.
	 * <p>
	 * Character encoding names can be found at
	 * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
	 * <p>
	 * This method uses {@link String#String(byte[], String)}.
	 * 
	 * @param data
	 *            the byte array to write, do not modify during output, null
	 *            ignored
	 * @param output
	 *            the <code>Writer</code> to write to
	 * @param encoding
	 *            the encoding to use, null means platform default
	 * @throws NullPointerException
	 *             if output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void write(byte[] data, Writer output, String encoding) throws IOException {
		if (data != null) {
			if (encoding == null) {
				write(data, output);
			} else {
				output.write(new String(data, encoding));
			}
		}
	}

	// write char[]
	// -----------------------------------------------------------------------
	/**
	 * Writes chars from a <code>char[]</code> to a <code>Writer</code> using
	 * the default character encoding of the platform.
	 * 
	 * @param data
	 *            the char array to write, do not modify during output, null
	 *            ignored
	 * @param output
	 *            the <code>Writer</code> to write to
	 * @throws NullPointerException
	 *             if output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void write(char[] data, Writer output) throws IOException {
		if (data != null) {
			output.write(data);
		}
	}

	/**
	 * Writes chars from a <code>char[]</code> to bytes on an
	 * <code>OutputStream</code>.
	 * <p>
	 * This method uses {@link String#String(char[])} and
	 * {@link String#getBytes()}.
	 * 
	 * @param data
	 *            the char array to write, do not modify during output, null
	 *            ignored
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @throws NullPointerException
	 *             if output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void write(char[] data, OutputStream output) throws IOException {
		if (data != null) {
			output.write(new String(data).getBytes());
		}
	}

	/**
	 * Writes chars from a <code>char[]</code> to bytes on an
	 * <code>OutputStream</code> using the specified character encoding.
	 * <p>
	 * Character encoding names can be found at
	 * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
	 * <p>
	 * This method uses {@link String#String(char[])} and
	 * {@link String#getBytes(String)}.
	 * 
	 * @param data
	 *            the char array to write, do not modify during output, null
	 *            ignored
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @param encoding
	 *            the encoding to use, null means platform default
	 * @throws NullPointerException
	 *             if output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void write(char[] data, OutputStream output, String encoding) throws IOException {
		if (data != null) {
			if (encoding == null) {
				write(data, output);
			} else {
				output.write(new String(data).getBytes(encoding));
			}
		}
	}

	// write CharSequence
	// -----------------------------------------------------------------------
	/**
	 * Writes chars from a <code>CharSequence</code> to a <code>Writer</code>.
	 * 
	 * @param data
	 *            the <code>CharSequence</code> to write, null ignored
	 * @param output
	 *            the <code>Writer</code> to write to
	 * @throws NullPointerException
	 *             if output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 2.0
	 */
	public static void write(CharSequence data, Writer output) throws IOException {
		if (data != null) {
			write(data.toString(), output);
		}
	}

	/**
	 * Writes chars from a <code>CharSequence</code> to bytes on an
	 * <code>OutputStream</code> using the default character encoding of the
	 * platform.
	 * <p>
	 * This method uses {@link String#getBytes()}.
	 * 
	 * @param data
	 *            the <code>CharSequence</code> to write, null ignored
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @throws NullPointerException
	 *             if output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 2.0
	 */
	public static void write(CharSequence data, OutputStream output) throws IOException {
		if (data != null) {
			write(data.toString(), output);
		}
	}

	/**
	 * Writes chars from a <code>CharSequence</code> to bytes on an
	 * <code>OutputStream</code> using the specified character encoding.
	 * <p>
	 * Character encoding names can be found at
	 * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
	 * <p>
	 * This method uses {@link String#getBytes(String)}.
	 * 
	 * @param data
	 *            the <code>CharSequence</code> to write, null ignored
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @param encoding
	 *            the encoding to use, null means platform default
	 * @throws NullPointerException
	 *             if output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 2.0
	 */
	public static void write(CharSequence data, OutputStream output, String encoding) throws IOException {
		if (data != null) {
			write(data.toString(), output, encoding);
		}
	}

	// write String
	// -----------------------------------------------------------------------
	/**
	 * Writes chars from a <code>String</code> to a <code>Writer</code>.
	 * 
	 * @param data
	 *            the <code>String</code> to write, null ignored
	 * @param output
	 *            the <code>Writer</code> to write to
	 * @throws NullPointerException
	 *             if output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void write(String data, Writer output) throws IOException {
		if (data != null) {
			output.write(data);
		}
	}

	/**
	 * Writes chars from a <code>String</code> to bytes on an
	 * <code>OutputStream</code> using the default character encoding of the
	 * platform.
	 * <p>
	 * This method uses {@link String#getBytes()}.
	 * 
	 * @param data
	 *            the <code>String</code> to write, null ignored
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @throws NullPointerException
	 *             if output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void write(String data, OutputStream output) throws IOException {
		if (data != null) {
			output.write(data.getBytes());
		}
	}

	/**
	 * Writes chars from a <code>String</code> to bytes on an
	 * <code>OutputStream</code> using the specified character encoding.
	 * <p>
	 * Character encoding names can be found at
	 * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
	 * <p>
	 * This method uses {@link String#getBytes(String)}.
	 * 
	 * @param data
	 *            the <code>String</code> to write, null ignored
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @param encoding
	 *            the encoding to use, null means platform default
	 * @throws NullPointerException
	 *             if output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void write(String data, OutputStream output, String encoding) throws IOException {
		if (data != null) {
			if (encoding == null) {
				write(data, output);
			} else {
				output.write(data.getBytes(encoding));
			}
		}
	}

	// writeLines
	// -----------------------------------------------------------------------
	/**
	 * Writes the <code>toString()</code> value of each item in a collection to
	 * an <code>OutputStream</code> line by line, using the default character
	 * encoding of the platform and the specified line ending.
	 *
	 * @param lines
	 *            the lines to write, null entries produce blank lines
	 * @param lineEnding
	 *            the line separator to use, null is system default
	 * @param output
	 *            the <code>OutputStream</code> to write to, not null, not
	 *            closed
	 * @throws NullPointerException
	 *             if the output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void writeLines(Collection<?> lines, String lineEnding, OutputStream output) throws IOException {
		if (lines == null) {
			return;
		}
		if (lineEnding == null) {
			lineEnding = LINE_SEPARATOR;
		}
		for (Object line : lines) {
			if (line != null) {
				output.write(line.toString().getBytes());
			}
			output.write(lineEnding.getBytes());
		}
	}

	/**
	 * Writes the <code>toString()</code> value of each item in a collection to
	 * an <code>OutputStream</code> line by line, using the specified character
	 * encoding and the specified line ending.
	 * <p>
	 * Character encoding names can be found at
	 * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
	 *
	 * @param lines
	 *            the lines to write, null entries produce blank lines
	 * @param lineEnding
	 *            the line separator to use, null is system default
	 * @param output
	 *            the <code>OutputStream</code> to write to, not null, not
	 *            closed
	 * @param encoding
	 *            the encoding to use, null means platform default
	 * @throws NullPointerException
	 *             if the output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void writeLines(Collection<?> lines, String lineEnding, OutputStream output, String encoding)
			throws IOException {
		if (encoding == null) {
			writeLines(lines, lineEnding, output);
		} else {
			if (lines == null) {
				return;
			}
			if (lineEnding == null) {
				lineEnding = LINE_SEPARATOR;
			}
			for (Object line : lines) {
				if (line != null) {
					output.write(line.toString().getBytes(encoding));
				}
				output.write(lineEnding.getBytes(encoding));
			}
		}
	}

	/**
	 * Writes the <code>toString()</code> value of each item in a collection to
	 * a <code>Writer</code> line by line, using the specified line ending.
	 *
	 * @param lines
	 *            the lines to write, null entries produce blank lines
	 * @param lineEnding
	 *            the line separator to use, null is system default
	 * @param writer
	 *            the <code>Writer</code> to write to, not null, not closed
	 * @throws NullPointerException
	 *             if the input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void writeLines(Collection<?> lines, String lineEnding, Writer writer) throws IOException {
		if (lines == null) {
			return;
		}
		if (lineEnding == null) {
			lineEnding = LINE_SEPARATOR;
		}
		for (Object line : lines) {
			if (line != null) {
				writer.write(line.toString());
			}
			writer.write(lineEnding);
		}
	}

	// copy from InputStream
	// -----------------------------------------------------------------------
	/**
	 * Copy bytes from an <code>InputStream</code> to an
	 * <code>OutputStream</code>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * <p>
	 * Large streams (over 2GB) will return a bytes copied value of
	 * <code>-1</code> after the copy has completed since the correct number of
	 * bytes cannot be returned as an int. For large streams use the
	 * <code>copyLarge(InputStream, OutputStream)</code> method.
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static int copy(InputStream input, OutputStream output) throws IOException {
		long count = copyLarge(input, output);
		if (count > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) count;
	}

	/**
	 * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
	 * <code>OutputStream</code>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * <p>
	 * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @return the number of bytes copied
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.3
	 */
	public static long copyLarge(InputStream input, OutputStream output) throws IOException {
		return copyLarge(input, output, new byte[DEFAULT_BUFFER_SIZE]);
	}

	/**
	 * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
	 * <code>OutputStream</code>.
	 * <p>
	 * This method uses the provided buffer, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * <p>
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @param buffer
	 *            the buffer to use for the copy
	 * @return the number of bytes copied
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 2.2
	 */
	public static long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {
		long count = 0;
		int n = 0;
		while (EOF != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	/**
	 * Copy some or all bytes from a large (over 2GB) <code>InputStream</code>
	 * to an <code>OutputStream</code>, optionally skipping input bytes.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * <p>
	 * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @param inputOffset
	 *            : number of bytes to skip from input before copying -ve values
	 *            are ignored
	 * @param length
	 *            : number of bytes to copy. -ve means all
	 * @return the number of bytes copied
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 2.2
	 */
	public static long copyLarge(InputStream input, OutputStream output, long inputOffset, long length)
			throws IOException {
		return copyLarge(input, output, inputOffset, length, new byte[DEFAULT_BUFFER_SIZE]);
	}

	/**
	 * Copy some or all bytes from a large (over 2GB) <code>InputStream</code>
	 * to an <code>OutputStream</code>, optionally skipping input bytes.
	 * <p>
	 * This method uses the provided buffer, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * <p>
	 * 
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @param inputOffset
	 *            : number of bytes to skip from input before copying -ve values
	 *            are ignored
	 * @param length
	 *            : number of bytes to copy. -ve means all
	 * @param buffer
	 *            the buffer to use for the copy
	 *
	 * @return the number of bytes copied
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 2.2
	 */
	public static long copyLarge(InputStream input, OutputStream output, final long inputOffset, final long length,
			byte[] buffer) throws IOException {
		if (inputOffset > 0) {
			skipFully(input, inputOffset);
		}
		if (length == 0) {
			return 0;
		}
		final int bufferLength = buffer.length;
		int bytesToRead = bufferLength;
		if (length > 0 && length < bufferLength) {
			bytesToRead = (int) length;
		}
		int read;
		long totalRead = 0;
		while (bytesToRead > 0 && EOF != (read = input.read(buffer, 0, bytesToRead))) {
			output.write(buffer, 0, read);
			totalRead += read;
			if (length > 0) { // only adjust length if not reading to the end
				// Note the cast must work because buffer.length is an integer
				bytesToRead = (int) Math.min(length - totalRead, bufferLength);
			}
		}
		return totalRead;
	}

	/**
	 * Copy bytes from an <code>InputStream</code> to chars on a
	 * <code>Writer</code> using the default character encoding of the platform.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * <p>
	 * This method uses {@link InputStreamReader}.
	 *
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param output
	 *            the <code>Writer</code> to write to
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void copy(InputStream input, Writer output) throws IOException {
		InputStreamReader in = new InputStreamReader(input);
		copy(in, output);
	}

	/**
	 * Copy bytes from an <code>InputStream</code> to chars on a
	 * <code>Writer</code> using the specified character encoding.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedInputStream</code>.
	 * <p>
	 * Character encoding names can be found at
	 * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
	 * <p>
	 * This method uses {@link InputStreamReader}.
	 *
	 * @param input
	 *            the <code>InputStream</code> to read from
	 * @param output
	 *            the <code>Writer</code> to write to
	 * @param encoding
	 *            the encoding to use, null means platform default
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void copy(InputStream input, Writer output, String encoding) throws IOException {
		if (encoding == null) {
			copy(input, output);
		} else {
			InputStreamReader in = new InputStreamReader(input, encoding);
			copy(in, output);
		}
	}

	// copy from Reader
	// -----------------------------------------------------------------------
	/**
	 * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedReader</code>.
	 * <p>
	 * Large streams (over 2GB) will return a chars copied value of
	 * <code>-1</code> after the copy has completed since the correct number of
	 * chars cannot be returned as an int. For large streams use the
	 * <code>copyLarge(Reader, Writer)</code> method.
	 *
	 * @param input
	 *            the <code>Reader</code> to read from
	 * @param output
	 *            the <code>Writer</code> to write to
	 * @return the number of characters copied, or -1 if &gt; Integer.MAX_VALUE
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static int copy(Reader input, Writer output) throws IOException {
		long count = copyLarge(input, output);
		if (count > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) count;
	}

	/**
	 * Copy chars from a large (over 2GB) <code>Reader</code> to a
	 * <code>Writer</code>.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedReader</code>.
	 * <p>
	 * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
	 *
	 * @param input
	 *            the <code>Reader</code> to read from
	 * @param output
	 *            the <code>Writer</code> to write to
	 * @return the number of characters copied
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.3
	 */
	public static long copyLarge(Reader input, Writer output) throws IOException {
		return copyLarge(input, output, new char[DEFAULT_BUFFER_SIZE]);
	}

	/**
	 * Copy chars from a large (over 2GB) <code>Reader</code> to a
	 * <code>Writer</code>.
	 * <p>
	 * This method uses the provided buffer, so there is no need to use a
	 * <code>BufferedReader</code>.
	 * <p>
	 *
	 * @param input
	 *            the <code>Reader</code> to read from
	 * @param output
	 *            the <code>Writer</code> to write to
	 * @param buffer
	 *            the buffer to be used for the copy
	 * @return the number of characters copied
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 2.2
	 */
	public static long copyLarge(Reader input, Writer output, char[] buffer) throws IOException {
		long count = 0;
		int n = 0;
		while (EOF != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	/**
	 * Copy some or all chars from a large (over 2GB) <code>InputStream</code>
	 * to an <code>OutputStream</code>, optionally skipping input chars.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedReader</code>.
	 * <p>
	 * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
	 * 
	 * @param input
	 *            the <code>Reader</code> to read from
	 * @param output
	 *            the <code>Writer</code> to write to
	 * @param inputOffset
	 *            : number of chars to skip from input before copying -ve values
	 *            are ignored
	 * @param length
	 *            : number of chars to copy. -ve means all
	 * @return the number of chars copied
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 2.2
	 */
	public static long copyLarge(Reader input, Writer output, final long inputOffset, final long length)
			throws IOException {
		return copyLarge(input, output, inputOffset, length, new char[DEFAULT_BUFFER_SIZE]);
	}

	/**
	 * Copy some or all chars from a large (over 2GB) <code>InputStream</code>
	 * to an <code>OutputStream</code>, optionally skipping input chars.
	 * <p>
	 * This method uses the provided buffer, so there is no need to use a
	 * <code>BufferedReader</code>.
	 * <p>
	 * 
	 * @param input
	 *            the <code>Reader</code> to read from
	 * @param output
	 *            the <code>Writer</code> to write to
	 * @param inputOffset
	 *            : number of chars to skip from input before copying -ve values
	 *            are ignored
	 * @param length
	 *            : number of chars to copy. -ve means all
	 * @param buffer
	 *            the buffer to be used for the copy
	 * @return the number of chars copied
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 2.2
	 */
	public static long copyLarge(Reader input, Writer output, final long inputOffset, final long length, char[] buffer)
			throws IOException {
		if (inputOffset > 0) {
			skipFully(input, inputOffset);
		}
		if (length == 0) {
			return 0;
		}
		int bytesToRead = buffer.length;
		if (length > 0 && length < buffer.length) {
			bytesToRead = (int) length;
		}
		int read;
		long totalRead = 0;
		while (bytesToRead > 0 && EOF != (read = input.read(buffer, 0, bytesToRead))) {
			output.write(buffer, 0, read);
			totalRead += read;
			if (length > 0) { // only adjust length if not reading to the end
				// Note the cast must work because buffer.length is an integer
				bytesToRead = (int) Math.min(length - totalRead, buffer.length);
			}
		}
		return totalRead;
	}

	/**
	 * Copy chars from a <code>Reader</code> to bytes on an
	 * <code>OutputStream</code> using the default character encoding of the
	 * platform, and calling flush.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedReader</code>.
	 * <p>
	 * Due to the implementation of OutputStreamWriter, this method performs a
	 * flush.
	 * <p>
	 * This method uses {@link OutputStreamWriter}.
	 *
	 * @param input
	 *            the <code>Reader</code> to read from
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void copy(Reader input, OutputStream output) throws IOException {
		OutputStreamWriter out = new OutputStreamWriter(output);
		copy(input, out);
		// XXX Unless anyone is planning on rewriting OutputStreamWriter, we
		// have to flush here.
		out.flush();
	}

	/**
	 * Copy chars from a <code>Reader</code> to bytes on an
	 * <code>OutputStream</code> using the specified character encoding, and
	 * calling flush.
	 * <p>
	 * This method buffers the input internally, so there is no need to use a
	 * <code>BufferedReader</code>.
	 * <p>
	 * Character encoding names can be found at
	 * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
	 * <p>
	 * Due to the implementation of OutputStreamWriter, this method performs a
	 * flush.
	 * <p>
	 * This method uses {@link OutputStreamWriter}.
	 *
	 * @param input
	 *            the <code>Reader</code> to read from
	 * @param output
	 *            the <code>OutputStream</code> to write to
	 * @param encoding
	 *            the encoding to use, null means platform default
	 * @throws NullPointerException
	 *             if the input or output is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static void copy(Reader input, OutputStream output, String encoding) throws IOException {
		if (encoding == null) {
			copy(input, output);
		} else {
			OutputStreamWriter out = new OutputStreamWriter(output, encoding);
			copy(input, out);
			// XXX Unless anyone is planning on rewriting OutputStreamWriter,
			// we have to flush here.
			out.flush();
		}
	}

	// content equals
	// -----------------------------------------------------------------------
	/**
	 * Compare the contents of two Streams to determine if they are equal or
	 * not.
	 * <p>
	 * This method buffers the input internally using
	 * <code>BufferedInputStream</code> if they are not already buffered.
	 *
	 * @param input1
	 *            the first stream
	 * @param input2
	 *            the second stream
	 * @return true if the content of the streams are equal or they both don't
	 *         exist, false otherwise
	 * @throws NullPointerException
	 *             if either input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static boolean contentEquals(InputStream input1, InputStream input2) throws IOException {
		if (!(input1 instanceof BufferedInputStream)) {
			input1 = new BufferedInputStream(input1);
		}
		if (!(input2 instanceof BufferedInputStream)) {
			input2 = new BufferedInputStream(input2);
		}

		int ch = input1.read();
		while (EOF != ch) {
			int ch2 = input2.read();
			if (ch != ch2) {
				return false;
			}
			ch = input1.read();
		}

		int ch2 = input2.read();
		return ch2 == EOF;
	}

	/**
	 * Compare the contents of two Readers to determine if they are equal or
	 * not.
	 * <p>
	 * This method buffers the input internally using
	 * <code>BufferedReader</code> if they are not already buffered.
	 *
	 * @param input1
	 *            the first reader
	 * @param input2
	 *            the second reader
	 * @return true if the content of the readers are equal or they both don't
	 *         exist, false otherwise
	 * @throws NullPointerException
	 *             if either input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 1.1
	 */
	public static boolean contentEquals(Reader input1, Reader input2) throws IOException {

		input1 = toBufferedReader(input1);
		input2 = toBufferedReader(input2);

		int ch = input1.read();
		while (EOF != ch) {
			int ch2 = input2.read();
			if (ch != ch2) {
				return false;
			}
			ch = input1.read();
		}

		int ch2 = input2.read();
		return ch2 == EOF;
	}

	/**
	 * Compare the contents of two Readers to determine if they are equal or
	 * not, ignoring EOL characters.
	 * <p>
	 * This method buffers the input internally using
	 * <code>BufferedReader</code> if they are not already buffered.
	 *
	 * @param input1
	 *            the first reader
	 * @param input2
	 *            the second reader
	 * @return true if the content of the readers are equal (ignoring EOL
	 *         differences), false otherwise
	 * @throws NullPointerException
	 *             if either input is null
	 * @throws IOException
	 *             if an I/O error occurs
	 * @since 2.2
	 */
	public static boolean contentEqualsIgnoreEOL(Reader input1, Reader input2) throws IOException {
		BufferedReader br1 = toBufferedReader(input1);
		BufferedReader br2 = toBufferedReader(input2);

		String line1 = br1.readLine();
		String line2 = br2.readLine();
		while (line1 != null && line2 != null && line1.equals(line2)) {
			line1 = br1.readLine();
			line2 = br2.readLine();
		}
		return line1 == null ? line2 == null ? true : false : line1.equals(line2);
	}

	/**
	 * Skip bytes from an input byte stream. This implementation guarantees that
	 * it will read as many bytes as possible before giving up; this may not
	 * always be the case for subclasses of {@link Reader}.
	 * 
	 * @param input
	 *            byte stream to skip
	 * @param toSkip
	 *            number of bytes to skip.
	 * @return number of bytes actually skipped.
	 * 
	 * @see InputStream#skip(long)
	 * 
	 * @throws IOException
	 *             if there is a problem reading the file
	 * @throws IllegalArgumentException
	 *             if toSkip is negative
	 * @since 2.0
	 */
	public static long skip(InputStream input, long toSkip) throws IOException {
		if (toSkip < 0) {
			throw new IllegalArgumentException("Skip count must be non-negative, actual: " + toSkip);
		}
		/*
		 * N.B. no need to synchronize this because: - we don't care if the
		 * buffer is created multiple times (the data is ignored) - we always
		 * use the same size buffer, so if it it is recreated it will still be
		 * OK (if the buffer size were variable, we would need to synch. to
		 * ensure some other thread did not create a smaller one)
		 */
		if (SKIP_BYTE_BUFFER == null) {
			SKIP_BYTE_BUFFER = new byte[SKIP_BUFFER_SIZE];
		}
		long remain = toSkip;
		while (remain > 0) {
			long n = input.read(SKIP_BYTE_BUFFER, 0, (int) Math.min(remain, SKIP_BUFFER_SIZE));
			if (n < 0) { // EOF
				break;
			}
			remain -= n;
		}
		return toSkip - remain;
	}

	/**
	 * Skip characters from an input character stream. This implementation
	 * guarantees that it will read as many characters as possible before giving
	 * up; this may not always be the case for subclasses of {@link Reader}.
	 * 
	 * @param input
	 *            character stream to skip
	 * @param toSkip
	 *            number of characters to skip.
	 * @return number of characters actually skipped.
	 * 
	 * @see Reader#skip(long)
	 * 
	 * @throws IOException
	 *             if there is a problem reading the file
	 * @throws IllegalArgumentException
	 *             if toSkip is negative
	 * @since 2.0
	 */
	public static long skip(Reader input, long toSkip) throws IOException {
		if (toSkip < 0) {
			throw new IllegalArgumentException("Skip count must be non-negative, actual: " + toSkip);
		}
		/*
		 * N.B. no need to synchronize this because: - we don't care if the
		 * buffer is created multiple times (the data is ignored) - we always
		 * use the same size buffer, so if it it is recreated it will still be
		 * OK (if the buffer size were variable, we would need to synch. to
		 * ensure some other thread did not create a smaller one)
		 */
		if (SKIP_CHAR_BUFFER == null) {
			SKIP_CHAR_BUFFER = new char[SKIP_BUFFER_SIZE];
		}
		long remain = toSkip;
		while (remain > 0) {
			long n = input.read(SKIP_CHAR_BUFFER, 0, (int) Math.min(remain, SKIP_BUFFER_SIZE));
			if (n < 0) { // EOF
				break;
			}
			remain -= n;
		}
		return toSkip - remain;
	}

	/**
	 * Skip the requested number of bytes or fail if there are not enough left.
	 * <p>
	 * This allows for the possibility that {@link InputStream#skip(long)} may
	 * not skip as many bytes as requested (most likely because of reaching
	 * EOF).
	 * 
	 * @param input
	 *            stream to skip
	 * @param toSkip
	 *            the number of bytes to skip
	 * @see InputStream#skip(long)
	 * 
	 * @throws IOException
	 *             if there is a problem reading the file
	 * @throws IllegalArgumentException
	 *             if toSkip is negative
	 * @throws EOFException
	 *             if the number of bytes skipped was incorrect
	 * @since 2.0
	 */
	public static void skipFully(InputStream input, long toSkip) throws IOException {
		if (toSkip < 0) {
			throw new IllegalArgumentException("Bytes to skip must not be negative: " + toSkip);
		}
		long skipped = skip(input, toSkip);
		if (skipped != toSkip) {
			throw new EOFException("Bytes to skip: " + toSkip + " actual: " + skipped);
		}
	}

	/**
	 * Skip the requested number of characters or fail if there are not enough
	 * left.
	 * <p>
	 * This allows for the possibility that {@link Reader#skip(long)} may not
	 * skip as many characters as requested (most likely because of reaching
	 * EOF).
	 * 
	 * @param input
	 *            stream to skip
	 * @param toSkip
	 *            the number of characters to skip
	 * @see Reader#skip(long)
	 * 
	 * @throws IOException
	 *             if there is a problem reading the file
	 * @throws IllegalArgumentException
	 *             if toSkip is negative
	 * @throws EOFException
	 *             if the number of characters skipped was incorrect
	 * @since 2.0
	 */
	public static void skipFully(Reader input, long toSkip) throws IOException {
		long skipped = skip(input, toSkip);
		if (skipped != toSkip) {
			throw new EOFException("Chars to skip: " + toSkip + " actual: " + skipped);
		}
	}

	/**
	 * Read characters from an input character stream. This implementation
	 * guarantees that it will read as many characters as possible before giving
	 * up; this may not always be the case for subclasses of {@link Reader}.
	 * 
	 * @param input
	 *            where to read input from
	 * @param buffer
	 *            destination
	 * @param offset
	 *            inital offset into buffer
	 * @param length
	 *            length to read, must be >= 0
	 * @return actual length read; may be less than requested if EOF was reached
	 * @throws IOException
	 *             if a read error occurs
	 * @since 2.2
	 */
	public static int read(Reader input, char[] buffer, int offset, int length) throws IOException {
		if (length < 0) {
			throw new IllegalArgumentException("Length must not be negative: " + length);
		}
		int remaining = length;
		while (remaining > 0) {
			int location = length - remaining;
			int count = input.read(buffer, offset + location, remaining);
			if (EOF == count) { // EOF
				break;
			}
			remaining -= count;
		}
		return length - remaining;
	}

	/**
	 * Read characters from an input character stream. This implementation
	 * guarantees that it will read as many characters as possible before giving
	 * up; this may not always be the case for subclasses of {@link Reader}.
	 * 
	 * @param input
	 *            where to read input from
	 * @param buffer
	 *            destination
	 * @return actual length read; may be less than requested if EOF was reached
	 * @throws IOException
	 *             if a read error occurs
	 * @since 2.2
	 */
	public static int read(Reader input, char[] buffer) throws IOException {
		return read(input, buffer, 0, buffer.length);
	}

	/**
	 * Read bytes from an input stream. This implementation guarantees that it
	 * will read as many bytes as possible before giving up; this may not always
	 * be the case for subclasses of {@link InputStream}.
	 * 
	 * @param input
	 *            where to read input from
	 * @param buffer
	 *            destination
	 * @param offset
	 *            inital offset into buffer
	 * @param length
	 *            length to read, must be >= 0
	 * @return actual length read; may be less than requested if EOF was reached
	 * @throws IOException
	 *             if a read error occurs
	 * @since 2.2
	 */
	public static int read(InputStream input, byte[] buffer, int offset, int length) throws IOException {
		if (length < 0) {
			throw new IllegalArgumentException("Length must not be negative: " + length);
		}
		int remaining = length;
		while (remaining > 0) {
			int location = length - remaining;
			int count = input.read(buffer, offset + location, remaining);
			if (EOF == count) { // EOF
				break;
			}
			remaining -= count;
		}
		return length - remaining;
	}

	/**
	 * Read bytes from an input stream. This implementation guarantees that it
	 * will read as many bytes as possible before giving up; this may not always
	 * be the case for subclasses of {@link InputStream}.
	 * 
	 * @param input
	 *            where to read input from
	 * @param buffer
	 *            destination
	 * @return actual length read; may be less than requested if EOF was reached
	 * @throws IOException
	 *             if a read error occurs
	 * @since 2.2
	 */
	public static int read(InputStream input, byte[] buffer) throws IOException {
		return read(input, buffer, 0, buffer.length);
	}

	/**
	 * Read the requested number of characters or fail if there are not enough
	 * left.
	 * <p>
	 * This allows for the possibility that
	 * {@link Reader#read(char[], int, int)} may not read as many characters as
	 * requested (most likely because of reaching EOF).
	 * 
	 * @param input
	 *            where to read input from
	 * @param buffer
	 *            destination
	 * @param offset
	 *            inital offset into buffer
	 * @param length
	 *            length to read, must be >= 0
	 * 
	 * @throws IOException
	 *             if there is a problem reading the file
	 * @throws IllegalArgumentException
	 *             if length is negative
	 * @throws EOFException
	 *             if the number of characters read was incorrect
	 * @since 2.2
	 */
	public static void readFully(Reader input, char[] buffer, int offset, int length) throws IOException {
		int actual = read(input, buffer, offset, length);
		if (actual != length) {
			throw new EOFException("Length to read: " + length + " actual: " + actual);
		}
	}

	/**
	 * Read the requested number of characters or fail if there are not enough
	 * left.
	 * <p>
	 * This allows for the possibility that
	 * {@link Reader#read(char[], int, int)} may not read as many characters as
	 * requested (most likely because of reaching EOF).
	 * 
	 * @param input
	 *            where to read input from
	 * @param buffer
	 *            destination
	 * 
	 * @throws IOException
	 *             if there is a problem reading the file
	 * @throws IllegalArgumentException
	 *             if length is negative
	 * @throws EOFException
	 *             if the number of characters read was incorrect
	 * @since 2.2
	 */
	public static void readFully(Reader input, char[] buffer) throws IOException {
		readFully(input, buffer, 0, buffer.length);
	}

	/**
	 * Read the requested number of bytes or fail if there are not enough left.
	 * <p>
	 * This allows for the possibility that
	 * {@link InputStream#read(byte[], int, int)} may not read as many bytes as
	 * requested (most likely because of reaching EOF).
	 * 
	 * @param input
	 *            where to read input from
	 * @param buffer
	 *            destination
	 * @param offset
	 *            inital offset into buffer
	 * @param length
	 *            length to read, must be >= 0
	 * 
	 * @throws IOException
	 *             if there is a problem reading the file
	 * @throws IllegalArgumentException
	 *             if length is negative
	 * @throws EOFException
	 *             if the number of bytes read was incorrect
	 * @since 2.2
	 */
	public static void readFully(InputStream input, byte[] buffer, int offset, int length) throws IOException {
		int actual = read(input, buffer, offset, length);
		if (actual != length) {
			throw new EOFException("Length to read: " + length + " actual: " + actual);
		}
	}

	/**
	 * Read the requested number of bytes or fail if there are not enough left.
	 * <p>
	 * This allows for the possibility that
	 * {@link InputStream#read(byte[], int, int)} may not read as many bytes as
	 * requested (most likely because of reaching EOF).
	 * 
	 * @param input
	 *            where to read input from
	 * @param buffer
	 *            destination
	 * 
	 * @throws IOException
	 *             if there is a problem reading the file
	 * @throws IllegalArgumentException
	 *             if length is negative
	 * @throws EOFException
	 *             if the number of bytes read was incorrect
	 * @since 2.2
	 */
	public static void readFully(InputStream input, byte[] buffer) throws IOException {
		readFully(input, buffer, 0, buffer.length);
	}

	public static String readContent(InputStream inputStream, String charsetName) {
		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(inputStream, charsetName);
			return IOUtils.read(isr, 256, 0);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			IOUtils.close(isr);
		}
	}

	/**
	 * 压缩
	 * 
	 * @param input
	 * @param out
	 * @throws IOException
	 */
	public static void gzipCompress(InputStream input, OutputStream out, int buffSize) throws IOException {
		// 使用默认缓冲区大小创建新的输出流
		GZIPOutputStream gzip = null;
		try {
			gzip = new GZIPOutputStream(out);
			write(input, gzip, buffSize);
		} finally {
			close(gzip);
		}
	}

	/**
	 * 压缩
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public static byte[] gzipCompress(byte[] data) throws IOException {
		UnsafeByteArrayInputStream input = new UnsafeByteArrayInputStream(data);
		UnsafeByteArrayOutputStream out = new UnsafeByteArrayOutputStream();
		try {
			gzipCompress(input, out, Math.min(data.length, DEFAULT_BUFFER_SIZE));
			return out.toByteArray();
		} finally {
			close(out, input);
		}
	}

	/**
	 * 解压
	 * 
	 * @param input
	 * @param out
	 * @param buffSize
	 * @throws IOException
	 */
	public static void gzipUnCompress(InputStream input, OutputStream out, int buffSize) throws IOException {
		// 使用默认缓冲区大小创建新的输入流
		GZIPInputStream gzip = null;
		try {
			gzip = new GZIPInputStream(input);
			write(gzip, out, buffSize);
		} finally {
			close(gzip);
		}
	}

	/**
	 * 解压
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public static byte[] gzipUnCompress(byte[] data) throws IOException {
		UnsafeByteArrayInputStream input = new UnsafeByteArrayInputStream(data);
		UnsafeByteArrayOutputStream out = new UnsafeByteArrayOutputStream();
		try {
			gzipUnCompress(input, out, Math.min(data.length, DEFAULT_BUFFER_SIZE));
			return out.toByteArray();
		} finally {
			close(out, input);
		}
	}

}
