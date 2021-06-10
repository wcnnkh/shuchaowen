package scw.sql;

import java.sql.SQLException;

import scw.util.Processor;

@FunctionalInterface
public interface SqlProcessor<S, T> extends Processor<S, T, SQLException> {
	T process(S source) throws SQLException;
}
