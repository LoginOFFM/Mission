package ru.sbt.task.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.sbt.task.model.entity.Contract;
import ru.sbt.task.model.entity.Point;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findByClientFullNameContainingIgnoreCase(String fullName);
    List<Contract> findByPointAndStatus(Point point, String status);
    List<Contract> findByPoint(Point point);
}
