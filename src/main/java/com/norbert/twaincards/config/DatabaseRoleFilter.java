package com.norbert.twaincards.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.norbert.twaincards.entity.User;
import com.norbert.twaincards.util.SecurityUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that sets the appropriate database role and user ID for each request
 */
@Component
@Slf4j
public class DatabaseRoleFilter extends OncePerRequestFilter {

    @Autowired
    private SecurityUtils securityUtils;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        try {
            if (securityUtils.isAuthenticated()) {
                User currentUser = securityUtils.getCurrentUser();
                String roleName = currentUser.getRole().getName();
                
                // Set current user ID for row-level security
                try {
                    jdbcTemplate.execute("SELECT set_config('app.user_id', '" + currentUser.getId() + "', false)");
                    log.debug("Set app.user_id to {}", currentUser.getId());
                } catch (Exception e) {
                    log.error("Failed to set app.user_id parameter: {}", e.getMessage());
                }
                
                // Set role-based datasource routing
                RoleBasedDataSourceRouter.setCurrentRole(roleName);
                log.debug("Set database role to {}", roleName);
            }
            
            filterChain.doFilter(request, response);
        } finally {
            // Clean up thread local
            RoleBasedDataSourceRouter.clearCurrentRole();
        }
    }
} 