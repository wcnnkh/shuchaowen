package scw.util.stream;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.BaseStream;

/**
 * 虽然可以自动关闭，并并非所有情况都适用，例如调用iterator/spliterator方法或获取到此对象后未调用任何方法
 * 
 * @author shuchaowen
 *
 * @param <T>
 * @param <S>
 */
public interface AutoCloseBaseStream<T, S extends BaseStream<T, S>> extends BaseStream<T, S>, Iterable<T> {
	/**
	 * 此方法不会自动关闭Stream
	 */
	@Override
	Iterator<T> iterator();

	/**
	 * 此方法不会自动关闭Stream
	 */
	@Override
	Spliterator<T> spliterator();
	
	boolean isClosed();
	
	@Override
	default void forEach(Consumer<? super T> action) {
		try {
			Iterable.super.forEach(action);
		} finally {
			close();
		}
	}
}
