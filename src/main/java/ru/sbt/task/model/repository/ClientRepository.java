package ru.sbt.task.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.sbt.task.model.entity.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {
}

