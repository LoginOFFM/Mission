package ru.sbt.task.model.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sbt.task.model.entity.Point;

public interface PointRepository extends JpaRepository<Point, Long> {
}

