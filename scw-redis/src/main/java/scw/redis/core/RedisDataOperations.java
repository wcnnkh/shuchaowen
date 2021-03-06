package scw.redis.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import scw.context.annotation.Provider;
import scw.core.utils.CollectionUtils;
import scw.data.DataOperations;
import scw.redis.core.RedisStringCommands.ExpireOption;

@Provider
public class RedisDataOperations implements DataOperations {
	private final Redis redisTemplete;

	public RedisDataOperations(Redis redisTemplate) {
		this.redisTemplete = redisTemplate;
	}

	@Override
	public boolean touch(String key, int exp) {
		return redisTemplete.touch(key) == 1;
	}

	@Override
	public boolean add(String key, int exp, Object value) {
		if (exp > 0) {
			Boolean b = redisTemplete.getObjectCommands().set(key, value, ExpireOption.EX, exp, SetOption.NX);
			return b == null ? false : b;
		} else {
			redisTemplete.getObjectCommands().setNX(key, value);
			return true;
		}
	}

	@Override
	public void set(String key, int exp, Object value) {
		if (exp > 0) {
			redisTemplete.getObjectCommands().setex(key, exp, value);
		} else {
			redisTemplete.getObjectCommands().set(key, value);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(String key) {
		return (T) redisTemplete.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Map<String, T> get(Collection<String> keys) {
		List<Object> list = redisTemplete.getObjectCommands().mget(keys.toArray(new String[0]));
		if (CollectionUtils.isEmpty(list)) {
			return Collections.emptyMap();
		}

		Map<String, T> map = new LinkedHashMap<String, T>(list.size());
		Iterator<String> keyIterator = keys.iterator();
		Iterator<Object> valueIterator = list.iterator();
		while (keyIterator.hasNext() && valueIterator.hasNext()) {
			map.put(keyIterator.next(), (T) valueIterator.next());
		}
		return map;
	}

	@Override
	public boolean add(String key, Object value) {
		Boolean v = redisTemplete.getObjectCommands().setNX(key, value);
		return v == null ? false : v;
	}

	@Override
	public void set(String key, Object value) {
		redisTemplete.getObjectCommands().set(key, value);
	}

	@Override
	public boolean isExist(String key) {
		return redisTemplete.exists(key) == 1;
	}

	@Override
	public boolean delete(String key) {
		return redisTemplete.del(key) == 1;
	}

	@Override
	public void delete(Collection<String> keys) {
		redisTemplete.del(keys.toArray(new String[0]));
	}

	@Override
	public long incr(String key, long delta, long initialValue, int exp) {
		return redisTemplete.incr(key, delta, initialValue, exp);
	}

	@Override
	public long decr(String key, long delta, long initialValue, int exp) {
		return redisTemplete.decr(key, delta, initialValue, exp);
	}

	@Override
	public long incr(String key, long delta) {
		return redisTemplete.incrBy(key, delta);
	}

	@Override
	public long incr(String key, long delta, long initialValue) {
		return redisTemplete.incr(key, delta, initialValue, 0);
	}

	@Override
	public long decr(String key, long delta) {
		return redisTemplete.decrBy(key, delta);
	}

	@Override
	public long decr(String key, long delta, long initialValue) {
		return redisTemplete.decr(key, delta, initialValue, 0);
	}

}
