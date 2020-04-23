package scw.orm;

import scw.lang.Description;
import scw.lang.NotSupportedException;

public abstract class AbstractColumn implements Column {
	private Class<?> declaringClass;

	public AbstractColumn(Class<?> declaringClass) {
		this.declaringClass = declaringClass;
	}

	public String getDescription() {
		Description description = getAnnotatedElement().getAnnotation(Description.class);
		return description == null ? null : description.value();
	}

	public Class<?> getDeclaringClass() {
		return declaringClass;
	}

	public ORMException createGetterORMException(Throwable e) {
		String message = "[ORM getter failed] - class=[" + getDeclaringClass() + "] column [" + getName() + "]";
		return e == null ? new ORMException(message) : new ORMException(message, e);
	}

	public ORMException createSetterORMException(Throwable e) {
		String message = "[ORM setter failed] - class=[" + getDeclaringClass() + "] column [" + getName() + "]";
		return e == null ? new ORMException(message) : new ORMException(message, e);
	}

	public NotSupportedException createNotSupportException() {
		return new NotSupportedException("[ORM failed] - class=[" + getDeclaringClass() + "] column [" + getName() + "]");
	}
}
