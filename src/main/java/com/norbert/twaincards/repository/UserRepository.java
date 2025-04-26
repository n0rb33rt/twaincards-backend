package com.norbert.twaincards.repository;
import com.norbert.twaincards.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторій для роботи з користувачами
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Пошук користувача за ім'ям
   * @param username ім'я користувача
   * @return опціональний об'єкт користувача
   */
  Optional<User> findByUsername(String username);

  /**
   * Пошук користувача за електронною поштою
   * @param email електронна пошта
   * @return опціональний об'єкт користувача
   */
  Optional<User> findByEmail(String email);

  /**
   * Перевірка існування користувача з вказаним ім'ям
   * @param username ім'я користувача
   * @return true, якщо користувач існує
   */
  boolean existsByUsername(String username);

  /**
   * Перевірка існування користувача з вказаною електронною поштою
   * @param email електронна пошта
   * @return true, якщо користувач існує
   */
  boolean existsByEmail(String email);

  /**
   * Знайти користувача з повною інформацією для автентифікації
   * @param usernameOrEmail ім'я користувача або електронна пошта
   * @return опціональний об'єкт користувача
   */
  @Query("SELECT u FROM User u LEFT JOIN FETCH u.nativeLanguage WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
  Optional<User> findByUsernameOrEmailWithNativeLanguage(@Param("usernameOrEmail") String usernameOrEmail);
}