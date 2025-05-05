package com.norbert.twaincards.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.norbert.twaincards.entity.Role;

/**
 * Dynamic datasource router that changes database connection based on user role
 */
public class RoleBasedDataSourceRouter extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> currentRole = new ThreadLocal<>();

    /**
     * Set the current role for the executing thread
     * 
     * @param role The role name to use for database connections
     */
    public static void setCurrentRole(String role) {
        currentRole.set(role);
    }

    /**
     * Clear the current role from thread local storage
     */
    public static void clearCurrentRole() {
        currentRole.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return currentRole.get();
    }
    
    /**
     * Create a role-based datasource that can route to different database users
     * 
     * @param env Spring environment for property access
     * @return Configured datasource router
     */
    public static DataSource createRoleBasedDataSource(Environment env) {
        RoleBasedDataSourceRouter dataSource = new RoleBasedDataSourceRouter();
        
        // Define data sources for each role
        DataSource userDataSource = createDataSource(env, "user");
        DataSource premiumDataSource = createDataSource(env, "premium");
        DataSource adminDataSource = createDataSource(env, "admin");
        DataSource defaultDataSource = createDefaultDataSource(env);
        
        // Set default data source for application startup
        dataSource.setDefaultTargetDataSource(defaultDataSource);
        
        // Map role names to data sources
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put(Role.ROLE_USER, userDataSource);
        dataSources.put(Role.ROLE_PREMIUM, premiumDataSource);
        dataSources.put(Role.ROLE_ADMIN, adminDataSource);
        
        dataSource.setTargetDataSources(dataSources);
        dataSource.afterPropertiesSet();
        
        return dataSource;
    }
    
    /**
     * Create a datasource for a specific role
     */
    private static DataSource createDataSource(Environment env, String role) {
        String url = env.getProperty("spring.datasource.url");
        String password = env.getProperty("spring.datasource." + role + ".password");
        
        return DataSourceBuilder.create()
            .url(url)
            .username("twaincards_" + role)
            .password(password)
            .driverClassName("org.postgresql.Driver")
            .build();
    }
    
    /**
     * Create the default datasource for application startup
     */
    private static DataSource createDefaultDataSource(Environment env) {
        return DataSourceBuilder.create()
            .url(env.getProperty("spring.datasource.url"))
            .username(env.getProperty("spring.datasource.username"))
            .password(env.getProperty("spring.datasource.password"))
            .driverClassName("org.postgresql.Driver")
            .build();
    }
} 