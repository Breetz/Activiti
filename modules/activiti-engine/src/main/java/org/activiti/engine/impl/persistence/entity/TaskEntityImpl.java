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
package org.activiti.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.BulkDeleteable;
import org.activiti.engine.impl.delegate.invocation.TaskListenerInvocation;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.task.DelegationState;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Falko Menge
 * @author Tijs Rademakers
 */
public class TaskEntityImpl extends VariableScopeImpl implements TaskEntity, Serializable, BulkDeleteable {

  public static final String DELETE_REASON_COMPLETED = "completed";
  public static final String DELETE_REASON_DELETED = "deleted";

  private static final long serialVersionUID = 1L;

  protected int revision;

  protected String owner;
  protected String assignee;
  protected String initialAssignee;
  protected DelegationState delegationState;

  protected String parentTaskId;

  protected String name;
  protected String description;
  protected int priority = DEFAULT_PRIORITY;
  protected Date createTime; // The time when the task has been created
  protected Date dueDate;
  protected int suspensionState = SuspensionState.ACTIVE.getStateCode();
  protected String category;

  protected boolean isIdentityLinksInitialized;
  protected List<IdentityLinkEntity> taskIdentityLinkEntities = new ArrayList<IdentityLinkEntity>();

  protected String executionId;
  protected ExecutionEntity execution;

  protected String processInstanceId;
  protected ExecutionEntity processInstance;

  protected String processDefinitionId;

  protected TaskDefinition taskDefinition;
  protected String taskDefinitionKey;
  protected String formKey;

  protected boolean isDeleted;

  protected String eventName;

  protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;

  protected List<VariableInstanceEntity> queryVariables;

  protected boolean forcedUpdate;

  public TaskEntityImpl() {
  }

  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<String, Object>();
    persistentState.put("assignee", this.assignee);
    persistentState.put("owner", this.owner);
    persistentState.put("name", this.name);
    persistentState.put("priority", this.priority);
    if (executionId != null) {
      persistentState.put("executionId", this.executionId);
    }
    if (processDefinitionId != null) {
      persistentState.put("processDefinitionId", this.processDefinitionId);
    }
    if (createTime != null) {
      persistentState.put("createTime", this.createTime);
    }
    if (description != null) {
      persistentState.put("description", this.description);
    }
    if (dueDate != null) {
      persistentState.put("dueDate", this.dueDate);
    }
    if (parentTaskId != null) {
      persistentState.put("parentTaskId", this.parentTaskId);
    }
    if (delegationState != null) {
      persistentState.put("delegationState", this.delegationState);
    }

    persistentState.put("suspensionState", this.suspensionState);

    if (forcedUpdate) {
      persistentState.put("forcedUpdate", Boolean.TRUE);
    }

    return persistentState;
  }

  public void delegate(String userId) {
    setDelegationState(DelegationState.PENDING);
    if (getOwner() == null) {
      setOwner(getAssignee());
    }
    setAssignee(userId, true, true);
  }

  public void resolve() {
    setDelegationState(DelegationState.RESOLVED);
    setAssignee(this.owner, true, true);
  }

  public int getRevisionNext() {
    return revision + 1;
  }

  public void forceUpdate() {
    this.forcedUpdate = true;
  }

  // variables //////////////////////////////////////////////////////////////////

  @Override
  protected VariableScopeImpl getParentVariableScope() {
    if (getExecution() != null) {
      return (ExecutionEntityImpl) execution;
    }
    return null;
  }

  @Override
  protected void initializeVariableInstanceBackPointer(VariableInstanceEntity variableInstance) {
    variableInstance.setTaskId(id);
    variableInstance.setExecutionId(executionId);
    variableInstance.setProcessInstanceId(processInstanceId);
  }

  @Override
  protected List<VariableInstanceEntity> loadVariableInstances() {
    return Context.getCommandContext().getVariableInstanceEntityManager().findVariableInstancesByTaskId(id);
  }

  @Override
  protected VariableInstanceEntity createVariableInstance(String variableName, Object value, ExecutionEntity sourceActivityExecution) {
    VariableInstanceEntity result = super.createVariableInstance(variableName, value, sourceActivityExecution);

    // Dispatch event, if needed
    if (Context.getProcessEngineConfiguration() != null && Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
      Context
          .getProcessEngineConfiguration()
          .getEventDispatcher()
          .dispatchEvent(
              ActivitiEventBuilder.createVariableEvent(ActivitiEventType.VARIABLE_CREATED, variableName, value, result.getType(), result.getTaskId(), result.getExecutionId(), getProcessInstanceId(),
                  getProcessDefinitionId()));
    }
    return result;
  }

  @Override
  protected void updateVariableInstance(VariableInstanceEntity variableInstance, Object value, ExecutionEntity sourceActivityExecution) {
    super.updateVariableInstance(variableInstance, value, sourceActivityExecution);

    // Dispatch event, if needed
    if (Context.getProcessEngineConfiguration() != null && Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
      Context
          .getProcessEngineConfiguration()
          .getEventDispatcher()
          .dispatchEvent(
              ActivitiEventBuilder.createVariableEvent(ActivitiEventType.VARIABLE_UPDATED, variableInstance.getName(), value, variableInstance.getType(), variableInstance.getTaskId(),
                  variableInstance.getExecutionId(), getProcessInstanceId(), getProcessDefinitionId()));
    }
  }

  // execution //////////////////////////////////////////////////////////////////

  public ExecutionEntity getExecution() {
    if ((execution == null) && (executionId != null)) {
      this.execution = Context.getCommandContext().getExecutionEntityManager().findById(executionId);
    }
    return execution;
  }

  public void setExecution(DelegateExecution execution) {
    if (execution != null) {
      this.execution = (ExecutionEntity) execution;
      this.executionId = this.execution.getId();
      this.processInstanceId = this.execution.getProcessInstanceId();
      this.processDefinitionId = this.execution.getProcessDefinitionId();

      Context.getCommandContext().getHistoryManager().recordTaskExecutionIdChange(this.id, executionId);

    } else {
      this.execution = null;
      this.executionId = null;
      this.processInstanceId = null;
      this.processDefinitionId = null;
    }
  }
  
  // task assignment ////////////////////////////////////////////////////////////

  @Override
  public void addCandidateUser(String userId) {
    Context.getCommandContext().getIdentityLinkEntityManager().addCandidateUser(this, userId);
  }

  @Override
  public void addCandidateUsers(Collection<String> candidateUsers) {
    Context.getCommandContext().getIdentityLinkEntityManager().addCandidateUsers(this, candidateUsers);
  }

  @Override
  public void addCandidateGroup(String groupId) {
    Context.getCommandContext().getIdentityLinkEntityManager().addCandidateGroup(this, groupId);
  }

  @Override
  public void addCandidateGroups(Collection<String> candidateGroups) {
    Context.getCommandContext().getIdentityLinkEntityManager().addCandidateGroups(this, candidateGroups);
  }

  @Override
  public void addUserIdentityLink(String userId, String identityLinkType) {
    Context.getCommandContext().getIdentityLinkEntityManager().addUserIdentityLink(this, userId, identityLinkType);
  }

  @Override
  public void addGroupIdentityLink(String groupId, String identityLinkType) {
    Context.getCommandContext().getIdentityLinkEntityManager().addGroupIdentityLink(this, groupId, identityLinkType);
  }
  
  public Set<IdentityLink> getCandidates() {
    Set<IdentityLink> potentialOwners = new HashSet<IdentityLink>();
    for (IdentityLinkEntity identityLinkEntity : getIdentityLinks()) {
      if (IdentityLinkType.CANDIDATE.equals(identityLinkEntity.getType())) {
        potentialOwners.add(identityLinkEntity);
      }
    }
    return potentialOwners;
  }

  public void deleteCandidateGroup(String groupId) {
    deleteGroupIdentityLink(groupId, IdentityLinkType.CANDIDATE);
  }

  public void deleteCandidateUser(String userId) {
    deleteUserIdentityLink(userId, IdentityLinkType.CANDIDATE);
  }

  public void deleteGroupIdentityLink(String groupId, String identityLinkType) {
    if (groupId != null) {
      Context.getCommandContext().getIdentityLinkEntityManager().deleteIdentityLink(this, null, groupId, identityLinkType);
    }
  }

  public void deleteUserIdentityLink(String userId, String identityLinkType) {
    if (userId != null) {
      Context.getCommandContext().getIdentityLinkEntityManager().deleteIdentityLink(this, userId, null, identityLinkType);
    }
  }

  public List<IdentityLinkEntity> getIdentityLinks() {
    if (!isIdentityLinksInitialized) {
      taskIdentityLinkEntities = Context.getCommandContext().getIdentityLinkEntityManager().findIdentityLinksByTaskId(id);
      isIdentityLinksInitialized = true;
    }

    return taskIdentityLinkEntities;
  }

  public void setExecutionVariables(Map<String, Object> parameters) {
    if (getExecution() != null) {
      execution.setVariables(parameters);
    }
  }

  // special setters (takes care of history) ////////////////////////////////////////////////////////////

  public void setName(String taskName) {
    this.name = taskName;

    CommandContext commandContext = Context.getCommandContext();
    if (commandContext != null) {
      commandContext.getHistoryManager().recordTaskNameChange(id, taskName);
    }
  }

  /* plain setter for persistence */
  public void setNameWithoutCascade(String taskName) {
    this.name = taskName;
  }

  public void setDescription(String description) {
    this.description = description;

    CommandContext commandContext = Context.getCommandContext();
    if (commandContext != null) {
      commandContext.getHistoryManager().recordTaskDescriptionChange(id, description);
    }
  }

  /* plain setter for persistence */
  public void setDescriptionWithoutCascade(String description) {
    this.description = description;
  }

  public void setAssignee(String assignee) {
    setAssignee(assignee, false, false);
  }

  public void setAssignee(String assignee, boolean dispatchAssignmentEvent, boolean dispatchUpdateEvent) {
    CommandContext commandContext = Context.getCommandContext();

    if (assignee == null && this.assignee == null) {

      // ACT-1923: even if assignee is unmodified and null, this should be
      // stored in history.
      if (commandContext != null) {
        commandContext.getHistoryManager().recordTaskAssigneeChange(id, assignee);
      }

      return;
    }
    this.assignee = assignee;

    // if there is no command context, then it means that the user is
    // calling the setAssignee outside a service method. E.g. while creating a new task.
    if (commandContext != null) {
      commandContext.getHistoryManager().recordTaskAssigneeChange(id, assignee);

      if (assignee != null && processInstanceId != null) {
        Context.getCommandContext().getIdentityLinkEntityManager().involveUser(getProcessInstance(), assignee, IdentityLinkType.PARTICIPANT);
      }

      if (!StringUtils.equals(initialAssignee, assignee)) {
        fireEvent(TaskListener.EVENTNAME_ASSIGNMENT);
        
        // TODO: this was like this in v5. Not sure if this is the right place?
        commandContext.getHistoryManager().recordTaskAssignment(this);
        
        initialAssignee = assignee;
      }

      if (commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
        if (dispatchAssignmentEvent) {
          commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.TASK_ASSIGNED, this));
        }

        if (dispatchUpdateEvent) {
          commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_UPDATED, this));
        }
      }
    }
  }

  /* plain setter for persistence */
  public void setAssigneeWithoutCascade(String assignee) {
    this.assignee = assignee;

    // Assign the assignee that was persisted before
    this.initialAssignee = assignee;
  }

  public void setOwner(String owner) {
    setOwner(owner, false);
  }

  public void setOwner(String owner, boolean dispatchUpdateEvent) {
    if (owner == null && this.owner == null) {
      return;
    }
    // if (owner!=null && owner.equals(this.owner)) {
    // return;
    // }
    this.owner = owner;

    CommandContext commandContext = Context.getCommandContext();
    if (commandContext != null) {
      commandContext.getHistoryManager().recordTaskOwnerChange(id, owner);

      if (owner != null && processInstanceId != null) {
        Context.getCommandContext().getIdentityLinkEntityManager().involveUser(getProcessInstance(), owner, IdentityLinkType.PARTICIPANT);
      }

      if (dispatchUpdateEvent && commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
        if (dispatchUpdateEvent) {
          commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_UPDATED, this));
        }
      }
    }
  }

  /* plain setter for persistence */
  public void setOwnerWithoutCascade(String owner) {
    this.owner = owner;
  }

  public void setDueDate(Date dueDate) {
    setDueDate(dueDate, false);
  }

  public void setDueDate(Date dueDate, boolean dispatchUpdateEvent) {
    this.dueDate = dueDate;

    CommandContext commandContext = Context.getCommandContext();
    if (commandContext != null) {
      commandContext.getHistoryManager().recordTaskDueDateChange(id, dueDate);

      if (dispatchUpdateEvent && commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
        if (dispatchUpdateEvent) {
          commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_UPDATED, this));
        }
      }
    }
  }

  public void setDueDateWithoutCascade(Date dueDate) {
    this.dueDate = dueDate;
  }

  public void setPriority(int priority) {
    setPriority(priority, false);
  }

  public void setPriority(int priority, boolean dispatchUpdateEvent) {
    this.priority = priority;

    CommandContext commandContext = Context.getCommandContext();
    if (commandContext != null) {
      commandContext.getHistoryManager().recordTaskPriorityChange(id, priority);

      if (dispatchUpdateEvent && commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
        if (dispatchUpdateEvent) {
          commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_UPDATED, this));
        }
      }
    }
  }

  public void setCategoryWithoutCascade(String category) {
    this.category = category;
  }

  public void setCategory(String category) {
    this.category = category;

    CommandContext commandContext = Context.getCommandContext();
    if (commandContext != null) {
      commandContext.getHistoryManager().recordTaskCategoryChange(id, category);
    }
  }

  public void setPriorityWithoutCascade(int priority) {
    this.priority = priority;
  }

  public void setParentTaskId(String parentTaskId) {
    this.parentTaskId = parentTaskId;

    CommandContext commandContext = Context.getCommandContext();
    if (commandContext != null) {
      commandContext.getHistoryManager().recordTaskParentTaskIdChange(id, parentTaskId);
    }
  }

  public void setParentTaskIdWithoutCascade(String parentTaskId) {
    this.parentTaskId = parentTaskId;
  }

  public void setTaskDefinitionKeyWithoutCascade(String taskDefinitionKey) {
    this.taskDefinitionKey = taskDefinitionKey;
  }

  public String getFormKey() {
    return formKey;
  }

  public void setFormKey(String formKey) {
    this.formKey = formKey;

    CommandContext commandContext = Context.getCommandContext();
    if (commandContext != null) {
      commandContext.getHistoryManager().recordTaskFormKeyChange(id, formKey);
    }
  }

  public void setFormKeyWithoutCascade(String formKey) {
    this.formKey = formKey;
  }

  public void fireEvent(String taskEventName) {
    TaskDefinition taskDefinition = getTaskDefinition();
    if (taskDefinition != null) {
      List<TaskListener> taskEventListeners = getTaskDefinition().getTaskListener(taskEventName);
      if (taskEventListeners != null) {
        for (TaskListener taskListener : taskEventListeners) {
          ExecutionEntity execution = getExecution();
          if (execution != null) {
            setEventName(taskEventName);
          }
          try {
            Context.getProcessEngineConfiguration().getDelegateInterceptor().handleInvocation(new TaskListenerInvocation(taskListener, (DelegateTask) this));
          } catch (Exception e) {
            throw new ActivitiException("Exception while invoking TaskListener: " + e.getMessage(), e);
          }
        }
      }
    }
  }
  
  // Override from VariableScopeImpl

  @Override
  protected boolean isActivityIdUsedForDetails() {
    return false;
  }

  // Overridden to avoid fetching *all* variables (as is the case in the super // call)
  @Override
  protected VariableInstanceEntity getSpecificVariable(String variableName) {
    CommandContext commandContext = Context.getCommandContext();
    if (commandContext == null) {
      throw new ActivitiException("lazy loading outside command context");
    }
    VariableInstanceEntity variableInstance = commandContext.getVariableInstanceEntityManager().findVariableInstanceByTaskAndName(id, variableName);

    return variableInstance;
  }

  @Override
  protected List<VariableInstanceEntity> getSpecificVariables(Collection<String> variableNames) {
    CommandContext commandContext = Context.getCommandContext();
    if (commandContext == null) {
      throw new ActivitiException("lazy loading outside command context");
    }
    return commandContext.getVariableInstanceEntityManager().findVariableInstancesByTaskAndNames(id, variableNames);
  }

  // modified getters and setters ///////////////////////////////////////////////

  public void setTaskDefinition(TaskDefinition taskDefinition) {
    this.taskDefinition = taskDefinition;
    this.taskDefinitionKey = taskDefinition.getKey();

    CommandContext commandContext = Context.getCommandContext();
    if (commandContext != null) {
      commandContext.getHistoryManager().recordTaskDefinitionKeyChange(this, taskDefinitionKey);
    }
  }

  public TaskDefinition getTaskDefinition() {
    if (taskDefinition == null && taskDefinitionKey != null) {
      ProcessDefinitionEntity processDefinition = Context.getProcessEngineConfiguration().getDeploymentManager().findDeployedProcessDefinitionById(processDefinitionId);
      taskDefinition = processDefinition.getTaskDefinitions().get(taskDefinitionKey);
    }
    return taskDefinition;
  }

  // regular getters and setters ////////////////////////////////////////////////////////

  public int getRevision() {
    return revision;
  }

  public void setRevision(int revision) {
    this.revision = revision;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Date getDueDate() {
    return dueDate;
  }

  public int getPriority() {
    return priority;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public String getExecutionId() {
    return executionId;
  }

  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  public String getAssignee() {
    return assignee;
  }
  
  public String getInitialAssignee() {
    return initialAssignee;
  }

  public String getTaskDefinitionKey() {
    return taskDefinitionKey;
  }

  public void setTaskDefinitionKey(String taskDefinitionKey) {
    this.taskDefinitionKey = taskDefinitionKey;

    CommandContext commandContext = Context.getCommandContext();
    if (commandContext != null) {
      commandContext.getHistoryManager().recordTaskDefinitionKeyChange(this, taskDefinitionKey);
    }
  }

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public ExecutionEntity getProcessInstance() {
    if (processInstance == null && processInstanceId != null) {
      processInstance = Context.getCommandContext().getExecutionEntityManager().findById(processInstanceId);
    }
    return processInstance;
  }

  public void setProcessInstance(ExecutionEntity processInstance) {
    this.processInstance = processInstance;
  }

  public void setExecution(ExecutionEntity execution) {
    this.execution = execution;
  }

  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public String getOwner() {
    return owner;
  }

  public DelegationState getDelegationState() {
    return delegationState;
  }

  public void setDelegationState(DelegationState delegationState) {
    this.delegationState = delegationState;
  }

  public String getDelegationStateString() {
    return (delegationState != null ? delegationState.toString() : null);
  }

  public void setDelegationStateString(String delegationStateString) {
    this.delegationState = (delegationStateString != null ? DelegationState.valueOf(DelegationState.class, delegationStateString) : null);
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  public void setDeleted(boolean isDeleted) {
    this.isDeleted = isDeleted;
  }

  public String getParentTaskId() {
    return parentTaskId;
  }

  public Map<String, VariableInstanceEntity> getVariableInstances() {
    ensureVariableInstancesInitialized();
    return variableInstances;
  }

  public int getSuspensionState() {
    return suspensionState;
  }

  public void setSuspensionState(int suspensionState) {
    this.suspensionState = suspensionState;
  }

  public String getCategory() {
    return category;
  }

  public boolean isSuspended() {
    return suspensionState == SuspensionState.SUSPENDED.getStateCode();
  }

  public Map<String, Object> getTaskLocalVariables() {
    Map<String, Object> variables = new HashMap<String, Object>();
    if (queryVariables != null) {
      for (VariableInstanceEntity variableInstance : queryVariables) {
        if (variableInstance.getId() != null && variableInstance.getTaskId() != null) {
          variables.put(variableInstance.getName(), variableInstance.getValue());
        }
      }
    }
    return variables;
  }

  public Map<String, Object> getProcessVariables() {
    Map<String, Object> variables = new HashMap<String, Object>();
    if (queryVariables != null) {
      for (VariableInstanceEntity variableInstance : queryVariables) {
        if (variableInstance.getId() != null && variableInstance.getTaskId() == null) {
          variables.put(variableInstance.getName(), variableInstance.getValue());
        }
      }
    }
    return variables;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public List<VariableInstanceEntity> getQueryVariables() {
    if (queryVariables == null && Context.getCommandContext() != null) {
      queryVariables = new VariableInitializingList();
    }
    return queryVariables;
  }

  public void setQueryVariables(List<VariableInstanceEntity> queryVariables) {
    this.queryVariables = queryVariables;
  }
  
  public String toString() {
    return "Task[id=" + id + ", name=" + name + "]";
  }

}