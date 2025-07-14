package ru.sbt.task.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.sbt.task.model.entity.Employee;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByLogin(String login);
}
