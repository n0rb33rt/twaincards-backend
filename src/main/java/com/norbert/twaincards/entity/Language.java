package com.norbert.twaincards.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Сутність, що представляє мову в системі
 */
@Entity
@Table(name = "languages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Language {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code", nullable = false, unique = true, length = 10)
  private String code;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "native_name", length = 50)
  private String nativeName;

}