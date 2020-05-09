package scw.data.generation;

/**
 * ID生成器
 * @author shuchaowen
 *
 * @param <T>
 */
public interface IdGenerator<T>{
	/**
	 * 获取下一个可用ID
	 * @return
	 */
	public T next();
}
