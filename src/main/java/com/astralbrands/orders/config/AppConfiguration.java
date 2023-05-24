package com.astralbrands.orders.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/*
	Class to build a database object to interact with the x3 database
 */
@Configuration
public class AppConfiguration {

	@Value("${x3.database.url}")
	String x3DBurl;

	@Bean(name = "x3DataSource")
	public DataSource x3DataSource() {
		DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
		//UPDATE PASSWORD
		dataSourceBuilder.url(x3DBurl);
		return dataSourceBuilder.build();
	}



}
