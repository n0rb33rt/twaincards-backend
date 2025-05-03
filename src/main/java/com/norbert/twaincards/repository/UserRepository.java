package com.norbert.twaincards.repository;

import com.norbert.twaincards.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  @Query("SELECT u FROM User u WHERE u.email = :usernameOrEmail OR u.username = :usernameOrEmail")
  Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);
}