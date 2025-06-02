package com.truist.batch.launcher;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manually boots the genericJob with the two flags. Only runs if
 * spring.batch.job.enabled=false
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ManualJobLauncher implements ApplicationRunner {

	private final JobLauncher jobLauncher;
	private final Job genericJob;

	@Value("${sourceSystem:}")
	private String sourceSystem;

	@Value("${jobName:}")
	private String jobName;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (sourceSystem.isEmpty() || jobName.isEmpty()) {
			log.info("No sourceSystem or jobName provided. Skipping job execution.");
			return;
		}

		try {
			// Added timestamp to make each run unique
			JobParameters jobParameters = new JobParametersBuilder().addString("sourceSystem", sourceSystem)
					.addString("jobName", jobName).addLong("timestamp", System.currentTimeMillis()).toJobParameters();

			log.info("🚀 Launching job with parameters: {}", jobParameters);

			JobExecution jobExecution = jobLauncher.run(genericJob, jobParameters);

			log.info("✅ Job finished with status: {}", jobExecution.getStatus());

		} catch (Exception e) {
			log.error("❌ Job execution failed", e);
			throw e;
		} finally {
			// ✅ CRITICAL: Force application shutdown
			log.info("🛑 Forcing application shutdown to prevent hanging...");

			// Give a small delay for logging to complete
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			// Force immediate shutdown
			System.exit(0);
		}
	}
}
