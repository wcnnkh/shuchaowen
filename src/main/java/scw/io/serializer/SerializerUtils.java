package scw.io.serializer;

import scw.core.Bits;
import scw.core.logger.LoggerUtils;
import scw.core.reflect.ReflectUtils;
import scw.core.utils.ClassUtils;
import scw.io.IOUtils;
import scw.io.UnsafeByteArrayOutputStream;
import scw.io.serializer.support.JavaSerializer;

public final class SerializerUtils {
	static {
		Class<?> serializerClass = null;
		String[] seralizerClassNames = { "scw.io.serializer.support.Hessian2Serializer",
				"scw.io.serializer.support.DubboHessian2Serializer", "scw.io.serializer.support.HessianSerializer",
				"scw.io.serializer.support.DubboHessianSerializer" };

		for (String name : seralizerClassNames) {
			try {
				serializerClass = Class.forName(name);
				break;
			} catch (Throwable e) {
			}
		}

		if (serializerClass == null) {
			serializerClass = JavaSerializer.class;
		}

		DEFAULT_SERIALIZER = (Serializer) ReflectUtils.newInstance(serializerClass);
		LoggerUtils.info(SerializerUtils.class, "default serializer：" + serializerClass.getName());
	}

	/**
	 * 默认的序列化实现
	 */
	public static final Serializer DEFAULT_SERIALIZER;

	private SerializerUtils() {
	}

	public static byte[] class2bytes(Class<?> clazz) {
		String name = clazz.getName();
		UnsafeByteArrayOutputStream byteArray = IOUtils.getUnsafeByteArrayOutputStream();
		for (char c : name.toCharArray()) {
			byte[] bs = { 0, 0 };
			Bits.putChar(bs, 0, c);
			byteArray.write(bs, 0, bs.length);
		}
		return byteArray.toByteArray();
	}

	public static Class<?> bytes2class(byte[] bytes) throws ClassNotFoundException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i += 2) {
			char c = Bits.getChar(bytes, i);
			sb.append(c);
		}
		return ClassUtils.forName(sb.toString());
	}

}
