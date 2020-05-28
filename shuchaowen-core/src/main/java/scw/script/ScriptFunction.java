package scw.script;

public interface ScriptFunction<T> {
	String getPrefix();

	String getSuffix();

	T eval(ScriptEngine<T> engine, String script) throws ScriptException;
}
