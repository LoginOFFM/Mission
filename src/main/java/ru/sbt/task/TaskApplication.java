package ru.sbt.task;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.sbt.task.model.entity.Employee;
import ru.sbt.task.model.repository.EmployeeRepository;

@SpringBootApplication
public class TaskApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskApplication.class, args);
	}
	@Bean
	CommandLineRunner initAdmin(EmployeeRepository employeeRepo, PasswordEncoder encoder) {
		return args -> {
			// Проверяем, существует ли уже admin
			if (employeeRepo.findByLogin("admin").isEmpty()) {
				Employee admin = new Employee();
				admin.setLogin("admin");
				admin.setPassword(encoder.encode("admin"));  // Шифруем пароль
				admin.setRole("ADMIN");
				admin.setFullName("Администратор");
				employeeRepo.save(admin);
				System.out.println("Создан администратор: admin/admin");
			}
		};
	}
}

