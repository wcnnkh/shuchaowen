package scw.context;

public interface ConfigurableClassesLoader extends ClassesLoader {
	void add(ClassesLoader classesLoader);

	void add(Class<?> clazz);
}
