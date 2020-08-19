package scw.http.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

import scw.http.HttpInputMessage;
import scw.net.message.converter.MessageConvertException;

public class ApacheFileItemParser implements FileItemParser {
	static {
		org.apache.commons.fileupload.FileItem.class.getName();
	}

	private FileUpload fileUpload;

	public ApacheFileItemParser() {
		this.fileUpload = new FileUpload(new DiskFileItemFactory());
	}

	public FileUpload getFileUpload() {
		return fileUpload;
	}

	public void setFileUpload(FileUpload fileUpload) {
		this.fileUpload = fileUpload;
	}

	public List<FileItem> parse(HttpInputMessage httpInputMessage)
			throws IOException {
		List<org.apache.commons.fileupload.FileItem> list;
		try {
			list = fileUpload.parseRequest(new HttpRequestContext(
					httpInputMessage));
		} catch (FileUploadException e) {
			throw new MessageConvertException(e);
		}

		List<FileItem> fileItems = new ArrayList<FileItem>(list.size());
		for (org.apache.commons.fileupload.FileItem fileItem : list) {
			fileItems.add(new FileItemWrapper(fileItem));
		}
		return fileItems;
	}

	private static class FileItemWrapper extends FileItem {
		private org.apache.commons.fileupload.FileItem fileItem;

		public FileItemWrapper(org.apache.commons.fileupload.FileItem fileItem) {
			super(fileItem.getFieldName());
			this.fileItem = fileItem;
			FileItemHeaders fileItemHeaders = fileItem.getHeaders();
			Iterator<String> iterator = fileItemHeaders.getHeaderNames();
			while (iterator.hasNext()) {
				String name = iterator.next();
				Iterator<String> valueIterator = fileItemHeaders
						.getHeaders(name);
				while (valueIterator.hasNext()) {
					getHeaders().add(name, valueIterator.next());
				}
			}
			getHeaders().readyOnly();
		}

		@Override
		public long getContentLength() {
			return fileItem.getSize();
		}

		@Override
		public String getTextBody() throws IOException {
			return fileItem.getString();
		}

		public InputStream getBody() throws IOException {
			return fileItem.getInputStream();
		}

		public void close() throws IOException {
			fileItem.delete();
		}
	}

	private static class HttpRequestContext implements RequestContext {
		private HttpInputMessage httpInputMessage;

		public HttpRequestContext(HttpInputMessage httpInputMessage) {
			this.httpInputMessage = httpInputMessage;
		}

		public String getCharacterEncoding() {
			return httpInputMessage.getContentType().getCharsetName();
		}

		public String getContentType() {
			return httpInputMessage.getContentType().toString();
		}

		public int getContentLength() {
			return (int) httpInputMessage.getContentLength();
		}

		public InputStream getInputStream() throws IOException {
			return httpInputMessage.getBody();
		}

	}
}
