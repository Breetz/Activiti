/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl.asyncexecutor.multitenant;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.impl.asyncexecutor.AsyncExecutor;
import org.activiti.engine.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.activiti.engine.impl.cfg.multitenant.TenantInfoHolder;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AsyncExecutor} that has one {@link AsyncExecutor} per tenant.
 * So each tenant has its own acquiring threads and it's own threadpool for executing jobs.
 * 
 * @author Joram Barrez
 */
public class ExecutorPerTenantAsyncExecutor implements TenantAwareAsyncExecutor {
  
  private static final Logger logger = LoggerFactory.getLogger(ExecutorPerTenantAsyncExecutor.class);
  
  protected TenantInfoHolder tenantInfoHolder;
  protected TenantAwareAsyncExecutorFactory tenantAwareAyncExecutorFactory;
  
  protected Map<String, AsyncExecutor> tenantExecutors = new HashMap<String, AsyncExecutor>();
  
  protected CommandExecutor commandExecutor;
  protected boolean active;
  protected boolean autoActivate;
  
  public ExecutorPerTenantAsyncExecutor(TenantInfoHolder tenantInfoHolder) {
    this(tenantInfoHolder, null);
  }
  
  public ExecutorPerTenantAsyncExecutor(TenantInfoHolder tenantInfoHolder, TenantAwareAsyncExecutorFactory tenantAwareAyncExecutorFactory) {
    this.tenantInfoHolder = tenantInfoHolder;
    this.tenantAwareAyncExecutorFactory = tenantAwareAyncExecutorFactory;
  }
  
  @Override
  public Set<String> getTenantIds() {
    return tenantExecutors.keySet();
  }

  @Override
  public void addTenantAsyncExecutor(String tenantId, boolean startExecutor) {
    AsyncExecutor tenantExecutor = null;
    
    if (tenantAwareAyncExecutorFactory == null) {
      tenantExecutor = new DefaultAsyncJobExecutor();
    } else {
      tenantExecutor = tenantAwareAyncExecutorFactory.createAsyncExecutor(tenantId);
    }
    
    if (tenantExecutor instanceof DefaultAsyncJobExecutor) {
      DefaultAsyncJobExecutor defaultAsyncJobExecutor = (DefaultAsyncJobExecutor) tenantExecutor;
      defaultAsyncJobExecutor.setAsyncJobsDueRunnable(new TenantAwareAcquireAsyncJobsDueRunnable(defaultAsyncJobExecutor, tenantInfoHolder, tenantId));
      defaultAsyncJobExecutor.setTimerJobRunnable(new TenantAwareAcquireTimerJobsRunnable(defaultAsyncJobExecutor, tenantInfoHolder, tenantId));
      defaultAsyncJobExecutor.setExecuteAsyncRunnableFactory(new TenantAwareExecuteAsyncRunnableFactory(tenantInfoHolder, tenantId));
    }
    
    tenantExecutor.setCommandExecutor(commandExecutor); // Needs to be done for job executors created after boot. Doesn't hurt on boot.
    
    tenantExecutors.put(tenantId, tenantExecutor);
    
    if (startExecutor) {
      tenantExecutor.start();
    }
  }
  
  @Override
  public void removeTenantAsyncExecutor(String tenantId) {
    shutdownTenantExecutor(tenantId);
    tenantExecutors.remove(tenantId);
  }
  
  protected AsyncExecutor determineAsyncExecutor() {
    return tenantExecutors.get(tenantInfoHolder.getCurrentTenantId());
  }

  @Override
  public boolean executeAsyncJob(JobEntity job) {
    return determineAsyncExecutor().executeAsyncJob(job);
  }

  @Override
  public void setCommandExecutor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
    for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
      asyncExecutor.setCommandExecutor(commandExecutor);
    }
  }

  @Override
  public CommandExecutor getCommandExecutor() {
    // Should never be accessed on this class, should be accessed on the actual AsyncExecutor
    throw new UnsupportedOperationException(); 
  }

  @Override
  public boolean isAutoActivate() {
    return autoActivate;
  }

  @Override
  public void setAutoActivate(boolean isAutoActivate) {
    autoActivate = isAutoActivate;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public void start() {
    for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
      asyncExecutor.start();
    }
    active = true;
  }

  @Override
  public synchronized void shutdown() {
    for (String tenantId : tenantExecutors.keySet()) {
      shutdownTenantExecutor(tenantId);
    }
    active = false;
  }

  protected void shutdownTenantExecutor(String tenantId) {
    logger.info("Shutting down async executor for tenant " + tenantId);
    tenantExecutors.get(tenantId).shutdown();
  }

  @Override
  public String getLockOwner() {
    return determineAsyncExecutor().getLockOwner();
  }

  @Override
  public int getTimerLockTimeInMillis() {
    return determineAsyncExecutor().getTimerLockTimeInMillis();
  }

  @Override
  public void setTimerLockTimeInMillis(int lockTimeInMillis) {
    for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
      asyncExecutor.setTimerLockTimeInMillis(lockTimeInMillis);
    }
  }

  @Override
  public int getAsyncJobLockTimeInMillis() {
    return determineAsyncExecutor().getAsyncJobLockTimeInMillis();
  }

  @Override
  public void setAsyncJobLockTimeInMillis(int lockTimeInMillis) {
    for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
      asyncExecutor.setAsyncJobLockTimeInMillis(lockTimeInMillis);
    }
  }

  @Override
  public int getDefaultTimerJobAcquireWaitTimeInMillis() {
    return determineAsyncExecutor().getDefaultTimerJobAcquireWaitTimeInMillis();
  }

  @Override
  public void setDefaultTimerJobAcquireWaitTimeInMillis(int waitTimeInMillis) {
    for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
      asyncExecutor.setDefaultTimerJobAcquireWaitTimeInMillis(waitTimeInMillis);
    }
  }

  @Override
  public int getDefaultAsyncJobAcquireWaitTimeInMillis() {
    return determineAsyncExecutor().getDefaultAsyncJobAcquireWaitTimeInMillis();
  }

  @Override
  public void setDefaultAsyncJobAcquireWaitTimeInMillis(int waitTimeInMillis) {
    for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
      asyncExecutor.setDefaultAsyncJobAcquireWaitTimeInMillis(waitTimeInMillis);
    }
  }
  
  @Override
  public int getDefaultQueueSizeFullWaitTimeInMillis() {
    return determineAsyncExecutor().getDefaultQueueSizeFullWaitTimeInMillis();
  }
  
  @Override
  public void setDefaultQueueSizeFullWaitTimeInMillis(int defaultQueueSizeFullWaitTime) {
    for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
      asyncExecutor.setDefaultQueueSizeFullWaitTimeInMillis(defaultQueueSizeFullWaitTime);
    }
  }

  @Override
  public int getMaxAsyncJobsDuePerAcquisition() {
    return determineAsyncExecutor().getMaxAsyncJobsDuePerAcquisition();
  }

  @Override
  public void setMaxAsyncJobsDuePerAcquisition(int maxJobs) {
    for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
      asyncExecutor.setMaxAsyncJobsDuePerAcquisition(maxJobs);
    }
  }

  @Override
  public int getMaxTimerJobsPerAcquisition() {
    return determineAsyncExecutor().getMaxTimerJobsPerAcquisition();
  }

  @Override
  public void setMaxTimerJobsPerAcquisition(int maxJobs) {
    for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
      asyncExecutor.setMaxTimerJobsPerAcquisition(maxJobs);
    }
  }

  @Override
  public int getRetryWaitTimeInMillis() {
    return determineAsyncExecutor().getRetryWaitTimeInMillis();
  }

  @Override
  public void setRetryWaitTimeInMillis(int retryWaitTimeInMillis) {
    for (AsyncExecutor asyncExecutor : tenantExecutors.values()) {
      asyncExecutor.setRetryWaitTimeInMillis(retryWaitTimeInMillis);
    }
  }

}
