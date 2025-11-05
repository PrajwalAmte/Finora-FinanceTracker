package com.finance_tracker;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FinanceTrackerApplication {

	public static void main(String[] args) {
		// Load .env file if it exists
		try {
			Dotenv dotenv = Dotenv.configure()
					.directory("./")
					.ignoreIfMissing()
					.load();
			
			// Set environment variables from .env file
			dotenv.entries().forEach(entry -> {
				System.setProperty(entry.getKey(), entry.getValue());
			});
		} catch (Exception e) {
			// .env file not found or error loading - continue with system environment variables
			System.out.println("Note: .env file not found, using system environment variables");
		}
		
		SpringApplication.run(FinanceTrackerApplication.class, args);
	}

}
