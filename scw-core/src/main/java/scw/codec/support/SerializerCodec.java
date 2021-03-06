package scw.codec.support;

import scw.codec.DecodeException;
import scw.codec.EncodeException;
import scw.io.JavaSerializer;
import scw.io.Serializer;
import scw.io.SerializerUtils;


public class SerializerCodec<T> implements BytesCodec<T>{
	public static final SerializerCodec<Object> DEFAULT = new SerializerCodec<Object>(SerializerUtils.getSerializer());
	public static final SerializerCodec<Object> JAVA = new SerializerCodec<Object>(JavaSerializer.INSTANCE);
	
	private final Serializer serializer;
	
	public SerializerCodec(Serializer serializer){
		this.serializer = serializer;
	}
	
	public byte[] encode(T source) throws EncodeException {
		return serializer.serialize(source);
	}

	public T decode(byte[] source) throws DecodeException {
		try {
			return serializer.deserialize(source);
		} catch (ClassNotFoundException e) {
			throw new DecodeException(e);
		}
	}

}
