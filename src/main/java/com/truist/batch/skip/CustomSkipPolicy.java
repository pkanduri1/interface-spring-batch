package com.truist.batch.skip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;

public class CustomSkipPolicy implements SkipPolicy {

    private static final Logger logger = LoggerFactory.getLogger(CustomSkipPolicy.class);

    private final int skipLimit;

    public CustomSkipPolicy(int skipLimit) {
        this.skipLimit = skipLimit;
    }




    @Override
    public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
        
        if (skipCount >= skipLimit) {
            logger.error("Error record skip limit reached: {} due to {}", skipCount, t.getMessage());
            return false;
        }

        if (t instanceof org.springframework.dao.DuplicateKeyException) {
            logger.info("Skipping DuplicateKeyException: {}", t.getMessage());
            return true;
        } else if (t instanceof java.sql.SQLException) {
            logger.info("Skipping SQLException (within limit): {}", t.getMessage());
            return true;
        }

        return false;
    }
}
