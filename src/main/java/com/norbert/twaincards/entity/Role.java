package com.norbert.twaincards.entity;

import lombok.*;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 20)
    private String name;

    public static final String ROLE_USER = "USER";
    public static final String ROLE_PREMIUM = "PREMIUM";
    public static final String ROLE_ADMIN = "ADMIN";
} 