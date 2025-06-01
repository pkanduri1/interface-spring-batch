package com.truist.batch.launcher;


import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Manually boots the genericJob with the two flags.
 * Only runs if spring.batch.job.enabled=false
 */
@Component
@ConditionalOnProperty(name = "spring.batch.job.enabled", havingValue = "false")
public class ManualJobLauncher implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final Job genericJob;

    @Autowired
    public ManualJobLauncher(JobLauncher jobLauncher, Job genericJob) {
        this.jobLauncher = jobLauncher;
        this.genericJob  = genericJob;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Grab your flags
        String sourceSystem = args.getOptionValues("sourceSystem").get(0);
        String jobName      = args.getOptionValues("jobName").get(0);

        // Build Spring Batch JobParameters
        JobParameters params = new JobParametersBuilder()
            .addString("sourceSystem", sourceSystem)
            .addString("jobName",      jobName)
            .toJobParameters();

        // Launch!
        jobLauncher.run(genericJob, params);
    }
}
