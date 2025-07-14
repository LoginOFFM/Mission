package ru.sbt.task.service;


import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sbt.task.model.entity.Employee;
import ru.sbt.task.model.repository.EmployeeRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public CustomUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByLogin(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Пользователь с логином '" + username + "' не найден")
                );

        // Проверяем, активен ли сотрудник
        return User.builder()
                .username(employee.getLogin())
                .password(employee.getPassword())
                .roles(employee.getRole().startsWith("ROLE_") ?
                        employee.getRole().substring(5) :
                        employee.getRole())
                .build();
    }
}
