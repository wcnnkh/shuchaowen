package scw.data.redis.jedis;

import redis.clients.jedis.util.SafeEncoder;
import scw.core.string.StringCodec;

public final class DefaultJedisStringCodec implements StringCodec {

	public byte[] encode(String text) {
		return SafeEncoder.encode(text);
	}

	public String decode(byte[] bytes) {
		return SafeEncoder.encode(bytes);
	}

}
