package ru.sbt.task.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.sbt.task.model.entity.Procuration;

public interface ProcurationRepository extends JpaRepository<Procuration, Long> {
}
