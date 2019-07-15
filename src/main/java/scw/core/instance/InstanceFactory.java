package scw.core.instance;

public interface InstanceFactory extends NoArgsInstanceFactory {
	/**
	 * 执行失败返回空或抛出异常
	 * @param name
	 * @param params
	 * @return
	 */
	<T> T getInstance(String name, Object... params);

	/**
	 * 执行失败返回空或抛出异常
	 * @param type
	 * @param params
	 * @return
	 */
	<T> T getInstance(Class<T> type, Object... params);

	/**
	 * 执行失败返回空或抛出异常
	 * @param name
	 * @param parameterTypes
	 * @param params
	 * @return
	 */
	<T> T getInstance(String name, Class<?>[] parameterTypes, Object... params);

	/**
	 * 执行失败返回空或抛出异常
	 * @param type
	 * @param parameterTypes
	 * @param params
	 * @return
	 */
	<T> T getInstance(Class<T> type, Class<?>[] parameterTypes, Object... params);
}