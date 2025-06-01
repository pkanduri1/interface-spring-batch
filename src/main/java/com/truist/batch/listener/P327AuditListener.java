/*
 * package com.truist.batch.listener;
 * 
 * import org.slf4j.Logger; import org.slf4j.LoggerFactory; import
 * org.springframework.batch.core.JobExecution; import
 * org.springframework.batch.core.JobExecutionListener; import
 * org.springframework.stereotype.Component;
 * 
 * @Component public class P327AuditListener implements JobExecutionListener {
 * 
 * private static final Logger logger =
 * LoggerFactory.getLogger(P327AuditListener.class);
 * 
 * @Override public void beforeJob(JobExecution jobExecution) {
 * logger.info("Job {} is starting with status {}",
 * jobExecution.getJobInstance().getJobName(), jobExecution.getStatus()); //
 * TODO: Add any auditing or setup logic before job starts }
 * 
 * @Override public void afterJob(JobExecution jobExecution) {
 * logger.info("Job {} has completed with status {}",
 * jobExecution.getJobInstance().getJobName(), jobExecution.getStatus()); //
 * TODO: Add any auditing or cleanup logic after job ends } }
 */