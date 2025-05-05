package com.norbert.twaincards.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * Configuration for database data sources
 */
@Configuration
public class DataSourceConfig {

    @Autowired
    private Environment env;
    
    /**
     * Create the primary data source that supports role-based routing
     * 
     * @return Configured data source
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        return RoleBasedDataSourceRouter.createRoleBasedDataSource(env);
    }
} 