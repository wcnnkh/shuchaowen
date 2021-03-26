package scw.env;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Logger;

import scw.codec.support.CharsetCodec;
import scw.core.utils.CollectionUtils;
import scw.core.utils.StringUtils;
import scw.env.support.DefaultEnvironment;
import scw.instance.support.DefaultServiceLoaderFactory;
import scw.util.EnumerationConvert;
import scw.util.MultiIterator;
import scw.value.StringValue;
import scw.value.Value;

public final class SystemEnvironment extends DefaultEnvironment {
	private static Logger logger = Logger.getLogger(SystemEnvironment.class.getName());
	public static final String PROPERTY_MAVEN_HOME = "maven.home";
	public static final String PROPERTY_PATH_SEPARATOR = "path.separator";
	public static final String PROPERTY_JAVA_CLASS_PATH = "java.class.path";
	public static final String PROPERTY_JAVA_IO_TMPDIR = "java.io.tmpdir";
	public static final String PROPERTY_USER_HOME = "user.home";
	public static final String PROPERTY_OS_NAME = "os.name";
	public static final String PROPERTY_USER_DIR = "user.dir";
	private static final String SYSTEM_ID_PROPERTY = "system.private.id";
	/**
	 * 为了兼容老版本
	 */
	public static final String WEB_ROOT_PROPERTY = "web.root";
	
	private static SystemEnvironment instance = new SystemEnvironment();
	
	static{
		instance.loadProperties("system.properties").register();
		instance.loadProperties(instance.getValue("system.properties.location", String.class, "/private.properties")).register();
		instance.loadServices(new DefaultServiceLoaderFactory(instance));
	}

	public static SystemEnvironment getInstance() {
		return instance;
	}

	private SystemEnvironment() {
		super(true);
		logger.info(getClassDirectory());
		String workPath = getWorkPath();
		logger.info("default " + WORK_PATH_PROPERTY + " in " + workPath);
	};

	@SuppressWarnings("unchecked")
	public Iterator<String> iterator() {
		return new MultiIterator<String>(super.iterator(),
				CollectionUtils
						.toIterator(EnumerationConvert.convertToStringEnumeration(System.getProperties().keys())),
				System.getenv().keySet().iterator());
	}
	
	public Value getValue(String key) {
		Value value = super.getValue(key);
		if(value != null){
			return value;
		}
		
		String v = getSystemProperty(key);
		return v == null? null:new StringValue(v);
	}
	
	public String getSystemProperty(String key){
		String value = System.getProperty(key);
		if (value == null) {
			value = System.getenv(key);
		}
		
		if(value == null && WEB_ROOT_PROPERTY.equals(key)){
			value = getWorkPath();
		}
		return value;
	}
	
	@Override
	public boolean containsKey(String key) {
		if(super.containsKey(key)){
			return true;
		}
		
		return getSystemProperty(key) != null;
	}

	/**
	 * 获取环境变量分割符
	 * 
	 * @return
	 */
	public String getPathSeparator() {
		return getString(PROPERTY_PATH_SEPARATOR);
	}

	public String getJavaClassPath() {
		return getString(PROPERTY_JAVA_CLASS_PATH);
	}

	public String[] getJavaClassPathArray() {
		String classPath = getJavaClassPath();
		if (StringUtils.isEmpty(classPath)) {
			return null;
		}

		return StringUtils.split(classPath, getPathSeparator());
	}

	public String getMavenHome() {
		return getString(PROPERTY_MAVEN_HOME);
	}

	public String getTempDirectoryPath() {
		return getString(PROPERTY_JAVA_IO_TMPDIR);
	}

	public String getUserHome() {
		return getString(PROPERTY_USER_HOME);
	}

	public String getOSName() {
		return getString(PROPERTY_OS_NAME);
	}
	
	public String getUserDir() {
		return getString(PROPERTY_USER_DIR);
	}
	
	public String getClassDirectory() {
		URL url = getClassLoader().getResource("");
		return url == null ? getUserDir() : url
				.getPath();
	}
	
	private String getDefaultWorkPath(){
		// /xxxxx/{project}/target/classes
		String path = getClassDirectory();
		File file = new File(path);
		if(file.isFile()){
			return null;
		}
		
		if (!file.getName().equals("classes")) {
			return path;
		}
		
		for(int i=0; i<2;i++){
			file = file.getParentFile();
			if(file == null){
				return path;
			}
			
			if(file.getName().equals("WEB-INF") && file.getParent() != null){
				return file.getParent();
			}
		}
		
		if (file != null) {
			File webapp = new File(file, "/src/main/webapp");
			if(webapp.exists()){
				return webapp.getPath();
			}
			/*
			//可能会出现一个bin目录，忽略此目录
			final File binDirectory = new File(file, "bin");
			// 路径/xxxx/src/main/webapp/WEB-INF 4层深度
			File wi = FileUtils.search(file, new Accept<File>() {
				
				public boolean accept(File e) {
					if(e.isDirectory() && "WEB-INF".equals(e.getName())){
						//可能会出现一个bin目录，忽略此目录
						if(binDirectory.exists() && binDirectory.isDirectory() && e.getPath().startsWith(binDirectory.getPath())){
							return false;
						}
						return true;
					}
					return false;
				}
			}, 4);
			if (wi != null) {
				return wi.getParent();
			}*/
		}
		return path;
	}
	
	@Override
	public String getWorkPath() {
		String path = super.getWorkPath();
		if(path == null){
			path = getDefaultWorkPath();
			if(path != null){
				setWorkPath(path);
			}
		}
		return path;
	}
	
	public String getPrivateId(){
		String systemOnlyId = getString(SYSTEM_ID_PROPERTY);
		if (StringUtils.isEmpty(systemOnlyId)) {
			systemOnlyId = CharsetCodec.UTF_8.toBase64().encode(getClassDirectory() + "&" + getWorkPath());
			if (systemOnlyId.endsWith("==")) {
				systemOnlyId = systemOnlyId.substring(0, systemOnlyId.length() - 2);
			}
			put(SYSTEM_ID_PROPERTY, systemOnlyId);
		}
		return systemOnlyId;
	}
}
