package ru.sbt.task.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Client client;
    private BigDecimal amount;
    private LocalDate term;
    @ManyToOne
    private Employee employee;
    private LocalDate issueDate;
    @ManyToOne
    private Point point;
    private String status;
}
