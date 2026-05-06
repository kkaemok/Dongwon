package org.kkaemok.dongwon.job.jobs;

import org.kkaemok.dongwon.job.JobType;

public final class NoneJob implements JobDefinition {
    @Override
    public JobType getType() {
        return JobType.NONE;
    }
}
