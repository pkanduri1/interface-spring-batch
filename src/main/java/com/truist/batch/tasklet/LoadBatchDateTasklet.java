package com.truist.batch.tasklet;

import java.util.Date;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class LoadBatchDateTasklet implements Tasklet {

  @Autowired
  private JdbcTemplate jdbc;

  @Value("#{jobParameters['sourceSystem']}")
  private String sourceSystem;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    Date batchDate = jdbc.queryForObject(
      "SELECT MAX(BATCH_DATE) FROM CM3INT.BATCH_DATE_LOCATOR",
      Date.class
    );
    // stash it in the JobExecutionContext for downstream steps
    chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("batchDate", batchDate);
    return RepeatStatus.FINISHED;
  }
}
