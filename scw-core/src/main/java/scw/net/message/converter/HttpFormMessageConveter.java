package scw.net.message.converter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;

import scw.core.utils.StringUtils;
import scw.http.HttpUtils;
import scw.http.MediaType;
import scw.net.MimeType;
import scw.net.message.InputMessage;
import scw.net.message.OutputMessage;
import scw.util.MultiValueMap;

public class HttpFormMessageConveter extends AbstractMessageConverter<Object> {

	public HttpFormMessageConveter() {
		supportMimeTypes.add(MediaType.APPLICATION_FORM_URLENCODED);
	}

	@Override
	public boolean support(Class<?> clazz) {
		if (Collection.class.isAssignableFrom(clazz)) {
			return false;
		}
		return true;
	}

	@Override
	protected Object readInternal(Type type, InputMessage inputMessage)
			throws IOException, MessageConvertException {
		String content = readTextBody(inputMessage);
		if (StringUtils.isEmpty(content)) {
			return null;
		}

		MultiValueMap<String, String> map = HttpUtils.toFormMap(content);
		String json = getJsonSupport().toJSONString(map);
		return getJsonSupport().parseObject(json, type);
	}

	@Override
	protected void writeInternal(Object body, MimeType contentType,
			OutputMessage outputMessage) throws IOException,
			MessageConvertException {
		writeTextBody(HttpUtils.toFormString(body, getCharset(outputMessage).name(),
				getJsonSupport()), contentType, outputMessage);
	}

}