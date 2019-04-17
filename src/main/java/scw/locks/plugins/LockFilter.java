package scw.locks.plugins;

import java.lang.reflect.Method;

import com.alibaba.fastjson.JSONArray;

import scw.aop.Filter;
import scw.aop.FilterChain;
import scw.aop.Invoker;
import scw.common.utils.StringUtils;
import scw.locks.Lock;
import scw.locks.LockFactory;
import scw.transaction.sql.cache.QueryCacheUtils;

/**
 * 实现方法级别的分布式锁
 * 
 * @author shuchaowen
 *
 */
public final class LockFilter implements Filter {
	private LockFactory lockFactory;

	public LockFilter(LockFactory lockFactory) {
		this(lockFactory, "");
	}

	public LockFilter(LockFactory lockFactory, String keyPrefix) {
		this.lockFactory = lockFactory;
	}

	public Object filter(Invoker invoker, Object proxy, Method method, Object[] args, FilterChain filterChain)
			throws Throwable {
		LockConfig lockConfig = method.getAnnotation(LockConfig.class);
		if (lockConfig == null) {
			return filterChain.doFilter(invoker, proxy, method, args);
		}

		StringBuilder sb = new StringBuilder(128);
		if (StringUtils.isEmpty(lockConfig.prefix())) {
			sb.append(method.toString());
		} else {
			sb.append(lockConfig.prefix());
		}

		if (lockConfig.keyIndex().length != 0) {
			sb.append("#");
			JSONArray jarr = new JSONArray();
			for (int index : lockConfig.keyIndex()) {
				jarr.add(args[index]);
			}
			sb.append(jarr.toJSONString());
		}

		String lockKey = sb.toString();
		Lock lock = lockFactory.getLock(lockKey);
		try {
			if (lockConfig.isWait()) {
				lock.lockWait();
			} else if (!lock.lock()) {
				throw new HasBeenLockedException(lockKey);
			}

			QueryCacheUtils.setQueryCacheEnable(false);
			return filterChain.doFilter(invoker, proxy, method, args);
		} finally {
			lock.unlock();
		}
	}
}