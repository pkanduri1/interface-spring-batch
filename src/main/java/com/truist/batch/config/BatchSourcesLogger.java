package com.truist.batch.config;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class BatchSourcesLogger {
  private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

  @PostConstruct
  public void logBatchSources() {
    try {
      Resource[] resources = resolver.getResources("classpath:/batch-sources/**/*.yml");
      Arrays.stream(resources).forEach(r ->
        {
			try {
				System.out.println("Found batch-source: " + r.getURI());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
      );
    } catch (IOException e) {
      System.err.println("No batch-source files found: " + e.getMessage());
    }
  }
}
