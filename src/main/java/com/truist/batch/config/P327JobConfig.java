/*
 * package com.truist.batch.config;
 * 
 * import java.util.Map; import java.util.Properties;
 * 
 * import org.springframework.batch.core.Job; import
 * org.springframework.batch.core.Step; import
 * org.springframework.batch.core.configuration.annotation.JobScope; import
 * org.springframework.batch.core.job.builder.JobBuilder; import
 * org.springframework.batch.core.launch.support.RunIdIncrementer; import
 * org.springframework.batch.core.repository.JobRepository; import
 * org.springframework.batch.core.step.builder.StepBuilder; import
 * org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.beans.factory.annotation.Value; import
 * org.springframework.context.annotation.Bean; import
 * org.springframework.context.annotation.Configuration; import
 * org.springframework.context.annotation.Primary; import
 * org.springframework.transaction.PlatformTransactionManager;
 * 
 * import com.truist.batch.listener.P327AuditListener; import
 * com.truist.batch.model.BatchJobProperties; import
 * com.truist.batch.processor.P327Processor; import
 * com.truist.batch.reader.P327Reader; import
 * com.truist.batch.util.YamlPropertyLoaderService; import
 * com.truist.batch.writer.P327Writer;
 * 
 * import lombok.extern.slf4j.Slf4j;
 * 
 * @Slf4j
 * 
 * @Configuration public class P327JobConfig {
 * 
 * @Autowired private P327Reader p327Reader;
 * 
 * @Autowired private P327Processor p327Processor;
 * 
 * @Autowired private P327Writer p327Writer;
 * 
 * @Autowired private BatchJobProperties batchJobProperties;
 * 
 * @Autowired private JobRepository jobRepository;
 * 
 * @Autowired private PlatformTransactionManager transactionManager;
 * 
 * @Autowired private DatabaseConfig databaseConfig;
 * 
 * @Autowired private TableNameValidator tableNameValidator;
 * 
 * @Autowired private P327AuditListener p327AuditListener;
 * 
 * @Bean
 * 
 * @Primary public Job p327Job() { return new JobBuilder("p327Job",
 * jobRepository).incrementer(new RunIdIncrementer()) .start(partionerStep())
 * .listener(p327AuditListener) .build(); }
 * 
 * @Bean private Step partionerStep() { return new
 * StepBuilder("partitionerStep", jobRepository) .partitioner("p327Step",
 * acctRangePartitioner(null, null)) .step(p327Step())
 * .gridSize(batchJobProperties.getGridSize()) .build(); }
 * 
 *//**
	 * @return
	 */
/*
 * @Bean private Step p327Step() { return new StepBuilder("p327Step",
 * jobRepository) .<Map<String, Object>, Map<String, String>>chunk(100,
 * transactionManager) .reader(p327Reader) .processor(p327Processor)
 * .writer(p327Writer) .listener(p327AuditListener) .faultTolerant() .build(); }
 *//**
	 * @param sourceSystem
	 * @param jobName
	 * @return
	 */
/*
 * @Bean
 * 
 * @JobScope public AcctRangePartitioner
 * acctRangePartitioner(@Value("#{jobParameters['sourceSystem']}") String
 * sourceSystem, @Value("#{jobParameters['jobName']}") String jobName) {
 * 
 * Properties properties = getPropertiesBySourceAndJob(sourceSystem, jobName);
 * return new AcctRangePartitioner(databaseConfig.dataSource(),
 * properties.getProperty("batch.tables.master"),
 * batchJobProperties.getGridSize(),
 * properties.getProperty("batch.columns.range-column"), tableNameValidator);
 * 
 * }
 * 
 *//**
	 * @param sourceSystem
	 * @param jobName
	 * @return
	 *//*
		 * private Properties getPropertiesBySourceAndJob(String sourceSystem, String
		 * jobName) { Properties p327SourceProperties =
		 * YamlPropertyLoaderService.loadProperties(jobName, sourceSystem); return
		 * p327SourceProperties; }
		 * 
		 * }
		 */