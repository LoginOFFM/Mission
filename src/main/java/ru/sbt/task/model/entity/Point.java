package ru.sbt.task.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Point {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String address;
    private String name;
}