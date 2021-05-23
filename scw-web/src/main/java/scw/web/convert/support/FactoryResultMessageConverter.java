package scw.web.convert.support;

import java.io.IOException;

import scw.context.result.Result;
import scw.context.result.ResultFactory;
import scw.convert.TypeDescriptor;
import scw.core.parameter.ParameterDescriptor;
import scw.web.ServerHttpRequest;
import scw.web.ServerHttpResponse;
import scw.web.convert.WebMessageConverter;
import scw.web.convert.WebMessageConverterAware;
import scw.web.convert.WebMessagelConverterException;
import scw.web.convert.annotation.FactoryResult;

public class FactoryResultMessageConverter implements WebMessageConverter, WebMessageConverterAware {
	private final ResultFactory resultFactory;
	private WebMessageConverter messageConverter;

	public FactoryResultMessageConverter(ResultFactory resultFactory) {
		this.resultFactory = resultFactory;
	}

	@Override
	public boolean canRead(ParameterDescriptor parameterDescriptor, ServerHttpRequest request) {
		return false;
	}

	@Override
	public Object read(ParameterDescriptor parameterDescriptor, ServerHttpRequest request)
			throws IOException, WebMessagelConverterException {
		return null;
	}

	@Override
	public void setWebMessageConverter(WebMessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	@Override
	public boolean canWrite(TypeDescriptor type, Object body) {
		if (body == null) {
			return false;
		}

		FactoryResult factoryResult = type.getAnnotation(FactoryResult.class);
		return !(body instanceof Result) && factoryResult != null && factoryResult.value();
	}

	@Override
	public void write(TypeDescriptor type, Object body, ServerHttpRequest request, ServerHttpResponse response)
			throws IOException, WebMessagelConverterException {
		Result result = resultFactory.success(body);
		messageConverter.write(type.narrow(result), result, request, response);
	}
}
