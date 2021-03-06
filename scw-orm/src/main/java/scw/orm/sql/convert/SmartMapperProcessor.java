package scw.orm.sql.convert;

import java.sql.ResultSet;
import java.sql.SQLException;

import scw.convert.ConversionService;
import scw.convert.TypeDescriptor;
import scw.env.Sys;
import scw.orm.ObjectRelationalMapping;
import scw.orm.OrmUtils;
import scw.orm.convert.PropertyFactoryToEntityConversionService;
import scw.sql.RowMapper;
import scw.sql.SqlUtils;
import scw.util.stream.Processor;

/**
 * 对ResultSet和实体之间的映射做了最大的兼容,支持任意对象的组合
 * 
 * @see ResultSetPropertyFactory
 * @see PropertyFactoryToEntityConversionService
 * @author shuchaowen
 *
 * @param <T>
 */
public class SmartMapperProcessor<T> implements RowMapper<T>, Processor<ResultSet, T, SQLException> {
	private final ConversionService conversionService;
	private final TypeDescriptor typeDescriptor;
	private final ObjectRelationalMapping objectRelationalMapping;

	public SmartMapperProcessor(TypeDescriptor typeDescriptor) {
		this(OrmUtils.getMapping(), typeDescriptor);
	}

	public SmartMapperProcessor(ObjectRelationalMapping objectRelationalMapping, TypeDescriptor typeDescriptor) {
		this(objectRelationalMapping, Sys.env.getConversionService(), typeDescriptor);
	}

	public SmartMapperProcessor(ObjectRelationalMapping objectRelationalMapping, ConversionService conversionService,
			TypeDescriptor typeDescriptor) {
		this.objectRelationalMapping = objectRelationalMapping;
		this.conversionService = conversionService;
		this.typeDescriptor = typeDescriptor;
	}

	@Override
	public T mapRow(ResultSet rs, int rowNum) throws SQLException {
		return process(rs);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T process(ResultSet rs) throws SQLException {
		if (typeDescriptor.isArray() || typeDescriptor.isCollection()) {
			Object[] array = SqlUtils.getRowValues(rs, rs.getMetaData().getColumnCount());
			return (T) conversionService.convert(array, TypeDescriptor.forObject(array), typeDescriptor);
		}

		if (objectRelationalMapping.isEntity(typeDescriptor.getType())) {
			// 如果是一个实体类
			ResultSetPropertyFactory propertyFactory = new ResultSetPropertyFactory(rs);
			return (T) conversionService.convert(propertyFactory, TypeDescriptor.forObject(propertyFactory),
					typeDescriptor);
		}

		int columnCount = rs.getMetaData().getColumnCount();
		if (columnCount == 0) {
			return null;
		}

		Object value = rs.getObject(1);
		return (T) conversionService.convert(value, TypeDescriptor.forObject(value), typeDescriptor);
	}
}
