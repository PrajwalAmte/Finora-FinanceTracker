package com.finance_tracker;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Paths;

@SpringBootApplication
public class FinanceTrackerApplication {

	private static final Logger log = LoggerFactory.getLogger(FinanceTrackerApplication.class);

	public static void main(String[] args) {

		try {
			String apiModulePath = Paths.get("").toAbsolutePath() + "/Finance_Tracker-API";

			Dotenv dotenv = Dotenv.configure()
					.directory(apiModulePath)
					.ignoreIfMissing()
					.load();

			dotenv.entries().forEach(entry -> {
				System.setProperty(entry.getKey(), entry.getValue());
			});

			log.info(".env loaded successfully from {}", apiModulePath);

		} catch (Exception e) {
			log.warn(".env not found — using system environment variables");
		}

		SpringApplication.run(FinanceTrackerApplication.class, args);
	}
}
