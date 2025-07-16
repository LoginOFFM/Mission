package ru.sbt.task.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.sbt.task.model.entity.Employee;
import ru.sbt.task.model.repository.EmployeeRepository;

@Service
public class SecurityService {
    private final EmployeeRepository employeeRepository;

    public SecurityService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    public Employee getCurrentEmployee() {
        String username = getCurrentUsername();
        if (username == null) return null;
        return employeeRepository.findByLogin(username).orElse(null);
    }
}