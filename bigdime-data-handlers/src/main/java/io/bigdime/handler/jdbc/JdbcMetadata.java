/**
 * Copyright (C) 2015 Stubhub.
 */
package io.bigdime.handler.jdbc;

import io.bigdime.adaptor.metadata.model.Attribute;
import io.bigdime.adaptor.metadata.model.Entitee;
import io.bigdime.adaptor.metadata.model.Metasegment;
import io.bigdime.alert.LoggerFactory;
import io.bigdime.core.commons.AdaptorLogger;
import io.bigdime.core.config.AdaptorConfig;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

/**
 * JdbcMetadata retrieves the metadata of fetched result set.
 * 
 * @author Pavan Sabinikari
 * 
 */

public class JdbcMetadata implements ResultSetExtractor<Metasegment> {

	private static final AdaptorLogger logger = new AdaptorLogger(
			LoggerFactory.getLogger(JdbcMetadata.class));
	@Autowired
	private JdbcInputDescriptor jdbcInputDescriptor;
	
	private Metasegment metasegment;
	private String tableName;

	public JdbcMetadata(JdbcInputDescriptor jdbcInputDescriptor){
		this.jdbcInputDescriptor = jdbcInputDescriptor;
	}
	
	public Metasegment extractData(ResultSet rs) throws SQLException,
			DataAccessException {
		if (rs != null) {
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			Set<Attribute> AttributesSet = new LinkedHashSet<Attribute>();
			for (int i = 1; i <= columnCount; i++) {
				String attributeType = rsmd.getColumnTypeName(i);
				Attribute attribute = new Attribute();
				attribute.setId(i);
				attribute.setAttributeName(rsmd.getColumnName(i));
				if(attributeType.equalsIgnoreCase(JdbcConstants.NUMBER)){
					if(rsmd.getPrecision(i) == 0){
						attributeType = JdbcConstants.NUMBER2DECIMAL;
					}
					if(rsmd.getScale(i) == 0){
						attributeType = JdbcConstants.NUMBER2BIGINT;
					}
					if(rsmd.getPrecision(i) != 0 && rsmd.getScale(i) != 0){
						attributeType = JdbcConstants.NUMBER2DECIMAL;
					}
				}
				if(attributeType.equalsIgnoreCase(JdbcConstants.CHAR)){
					if(rsmd.getPrecision(i) > 255){
						attributeType = JdbcConstants.CHAR2VARCHAR;
					}
				}
				attribute.setAttributeType(attributeType);
				attribute.setIntPart(rsmd.getPrecision(i)+"");
				attribute.setFractionalPart(rsmd.getScale(i)+"");
				attribute.setNullable("NO");
				attribute.setComment("Not Null");
				attribute.setFieldType(JdbcConstants.FILED_TYPE);
				AttributesSet.add(attribute);
				
				tableName = jdbcInputDescriptor.getEntityName();
				logger.debug("JDBC Reader Handler getting source Metadata ",
						"table Name: {} columnName:{} columnType:{} size:{}",tableName,
						rsmd.getColumnName(i), attributeType,
						rsmd.getColumnDisplaySize(i));
			}
 
			Set<Entitee> entitySet = new HashSet<Entitee>();
			Entitee entities = new Entitee();
			entities.setEntityName(tableName);
			entities.setEntityLocation(jdbcInputDescriptor.getEntityLocation());
			entities.setDescription("HDFS LOCATION");
			entities.setVersion(1.0);
			entities.setAttributes(AttributesSet);
			entitySet.add(entities);

			metasegment = new Metasegment();
			metasegment.setAdaptorName(AdaptorConfig.getInstance().getName());
			metasegment.setSchemaType("HIVE");
			metasegment.setEntitees(entitySet);
			metasegment.setDatabaseName(jdbcInputDescriptor.getTargetDBName());
			metasegment.setDatabaseLocation("");
			metasegment.setRepositoryType("TARGET");
			metasegment.setIsDataSource("Y");
			metasegment.setCreatedBy(AdaptorConfig.getInstance().getName());
			metasegment.setUpdatedBy(AdaptorConfig.getInstance().getName());
			
		}
		return metasegment;
	}

}