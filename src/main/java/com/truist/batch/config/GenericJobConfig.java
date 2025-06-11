package com.truist.batch.config;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import com.truist.batch.adapter.DataSourceAdapterRegistry;
import com.truist.batch.listener.GenericJobListener;
import com.truist.batch.listener.GenericStepListener;
import com.truist.batch.mapping.YamlMappingService;
import com.truist.batch.model.BatchJobProperties;
import com.truist.batch.model.FileConfig;
import com.truist.batch.partition.GenericPartitioner;
import com.truist.batch.processor.GenericProcessor;
import com.truist.batch.reader.GenericReader;
import com.truist.batch.tasklet.LoadBatchDateTasklet;
import com.truist.batch.writer.GenericWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class GenericJobConfig {

	private final YamlMappingService mappingService;
	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final BatchJobProperties config;
	private final GenericJobListener jobListener;
	private final GenericStepListener stepListener;
	private final TaskExecutor taskExecutor;
	private final LoadBatchDateTasklet loadBatchDateTasklet;
	//private final SimpleExecutionMonitor executionMonitor;
	private final DynamicBatchConfigLoader configLoader;
	
	// ✅ NEW: Inject the adapter registry 6/11/25	
		private final DataSourceAdapterRegistry adapterRegistry;

	@Bean
	public Step loadBatchDateStep() {
		return new StepBuilder("loadBatchDateStep", jobRepository)
				.tasklet(loadBatchDateTasklet, transactionManager)
				.build();
	}

	@Bean
	public Job genericJob() {
		return new org.springframework.batch.core.job.builder.JobBuilder("genericJob", jobRepository)
				.incrementer(new RunIdIncrementer())
				.listener(jobListener)
				//.listener(executionMonitor)
				.start(loadBatchDateStep())
				.next(filePartitionStep(null, null))
				.build();
	}

	/**
	 * Creates a partitioned step that distributes work across multiple threads.
	 * Each partition processes a different transaction type from the YAML mappings.
	 */
	@Bean
	@JobScope
	public Step filePartitionStep(
			@Value("#{jobParameters['jobName']}") String jobName,
			@Value("#{jobParameters['sourceSystem']}") String sourceSystem) {
		
		log.info("🔥 Creating partition step for job: {}.{}", sourceSystem, jobName);

		try {
			// Load dynamic configuration
			var systemConfig = configLoader.getSourceSystemConfig(sourceSystem);
			var jobConfig = configLoader.getJobConfig(sourceSystem, jobName);

			log.info("✅ Loaded configuration for {}.{}", sourceSystem, jobName);

			// Create partitioner with context
			GenericPartitioner partitioner = new GenericPartitioner(
				mappingService, 
				systemConfig, 
				jobConfig,
				sourceSystem,
				jobName        
			);

			// Create partitioned step - key change: use string name, not bean reference
			return new StepBuilder(jobName + "PartitionStep", jobRepository)
					.partitioner("workerStep", partitioner)  // ← String reference
					.step(createWorkerStep(jobName))          // ← Method call, not bean
					.gridSize(config.getGridSize())
					.taskExecutor(taskExecutor)
					.build();

		} catch (Exception e) {
			log.error("❌ Failed to create partition step for {}.{}", sourceSystem, jobName, e);
			throw new RuntimeException("Configuration loading failed", e);
		}
	}

	/**
	 * Creates the worker step that will be executed by each partition.
	 * This is NOT a Spring bean - it's a method that creates Step instances.
	 */
	private Step createWorkerStep(String jobName) {
		String stepName = jobName + "WorkerStep";
		
		return new StepBuilder(stepName, jobRepository)
				.<Map<String, Object>, Map<String, Object>>chunk(config.getChunkSize(), transactionManager)
				.reader(genericReader(null))   // These ARE beans and @StepScope
				.processor(genericProcessor(null))   // These ARE beans and @StepScope  
				.writer(genericWriter(null))         // These ARE beans and @StepScope
				.listener(stepListener)
				.faultTolerant()
				.skipLimit(10)
				.skip(Exception.class)
				.retryLimit(3)
				.retry(Exception.class)
				.build();
	}

	// ===== STEP-SCOPED BEANS (Keep these as-is) =====

	@Bean
	@StepScope
	public GenericReader genericReader(@Value("#{stepExecutionContext['fileConfig']}") FileConfig fileConfig) {
		return new GenericReader(fileConfig, adapterRegistry);
	}

	@Bean
	@StepScope
	public GenericProcessor genericProcessor(@Value("#{stepExecutionContext['fileConfig']}") FileConfig fileConfig) {
		return new GenericProcessor(fileConfig, mappingService);
	}

	@Bean
	@StepScope
	public GenericWriter genericWriter(@Value("#{stepExecutionContext['fileConfig']}") FileConfig fileConfig) {
		return new GenericWriter(mappingService, fileConfig);
	}
}