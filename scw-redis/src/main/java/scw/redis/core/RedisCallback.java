package scw.redis.core;

@FunctionalInterface
public interface RedisCallback<K, V, T> {
	T doInRedis(RedisCommands<K, V> commands) throws RedisSystemException;
}
