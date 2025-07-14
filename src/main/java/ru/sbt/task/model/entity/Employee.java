package ru.sbt.task.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    @ManyToOne
    private Procuration procuration;
    private String login;
    private String password;
    private String role;
}
