package scw.data.file;

import java.io.File;

import scw.core.Constants;
import scw.data.AbstractExpireCacheFactory;
import scw.data.ExpiredCache;
import scw.io.serialzer.NoTypeSpecifiedSerializer;
import scw.io.serialzer.SerializerUtils;
import scw.value.property.SystemPropertyFactory;

public class FileExpiredCacheFactory extends AbstractExpireCacheFactory{
	protected final NoTypeSpecifiedSerializer serializer;
	protected final String charsetName;
	protected final String cacheDirectory;

	protected FileExpiredCacheFactory() {
		this.serializer = SerializerUtils.DEFAULT_SERIALIZER;
		this.charsetName = Constants.DEFAULT_CHARSET_NAME;
		this.cacheDirectory = SystemPropertyFactory.getInstance().getTempDirectoryPath() + File.separator + getClass().getName();
	}

	public FileExpiredCacheFactory(String cacheDirectory) {
		this(SerializerUtils.DEFAULT_SERIALIZER, Constants.DEFAULT_CHARSET_NAME, cacheDirectory);
	}

	public FileExpiredCacheFactory(NoTypeSpecifiedSerializer serializer, String charsetName, String cacheDirectory) {
		this.serializer = serializer;
		this.charsetName = charsetName;
		this.cacheDirectory = cacheDirectory;
	}

	@Override
	protected ExpiredCache createExpiredCache(int exp) {
		return new FileCache(exp, serializer, charsetName, cacheDirectory + File.separator + exp);
	}
}