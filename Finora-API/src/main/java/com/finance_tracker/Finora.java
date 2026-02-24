package com.finance_tracker;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Paths;

@SpringBootApplication
public class Finora {

	private static final Logger log = LoggerFactory.getLogger(Finora.class);

	public static void main(String[] args) {

		try {
			String cwd = Paths.get("").toAbsolutePath().toString();
			String[] candidatePaths = { cwd, cwd + "/Finora-API" };

			Dotenv dotenv = null;
			for (String path : candidatePaths) {
				java.io.File envFile = new java.io.File(path + "/.env");
				if (envFile.exists()) {
					dotenv = Dotenv.configure()
							.directory(path)
							.ignoreIfMissing()
							.load();
					log.info(".env loaded successfully from {}", path);
					break;
				}
			}

			if (dotenv != null) {
				dotenv.entries().forEach(entry -> {
					System.setProperty(entry.getKey(), entry.getValue());
				});
			} else {
				log.warn(".env not found in {} — using system environment variables", String.join(" or ", candidatePaths));
			}

		} catch (Exception e) {
			log.warn(".env not found — using system environment variables");
		}

		SpringApplication.run(Finora.class, args);
	}
}
