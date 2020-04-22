package scw.security.ip;

import scw.core.instance.annotation.ResourceParameter;
import scw.core.parameter.annotation.DefaultValue;

/**
 * 白名单
 * 
 * 检查是否存在于白名单中
 * 
 * @author shuchaowen
 *
 */
public final class WhitelistIPVerification extends BaseIPVerification {
	private static final long serialVersionUID = 1L;

	public WhitelistIPVerification(@ResourceParameter@DefaultValue("ip-whitelist") String sourceFile) {
		appendIPFile(sourceFile);
	}

}
