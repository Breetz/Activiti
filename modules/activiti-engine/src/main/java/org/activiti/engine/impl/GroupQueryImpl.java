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

package org.activiti.engine.impl;

import java.util.List;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;

/**
 * @author Joram Barrez
 */
public class GroupQueryImpl extends AbstractQuery<GroupQuery, Group> implements GroupQuery {
  
  private static final long serialVersionUID = 1L;
  protected String id;
  protected String name;
  protected String nameLike;
  protected String type;
  protected String userId;
  protected String procDefId;
  

  public GroupQueryImpl() {
  }

  public GroupQueryImpl(CommandContext commandContext) {
    super(commandContext);
  }

  public GroupQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public GroupQuery groupId(String id) {
    if (id == null) {
      throw new ActivitiIllegalArgumentException("Provided id is null");
    }
    this.id = id;
    return this;
  }
  
  @Override
  public GroupQuery groupName(String name) {
    if (name == null) {
      throw new ActivitiIllegalArgumentException("Provided name is null");
    }
    this.name = name;
    return this;
  }
  
  @Override
  public GroupQuery groupNameLike(String nameLike) {
    if (nameLike == null) {
      throw new ActivitiIllegalArgumentException("Provided nameLike is null");
    }
    this.nameLike = nameLike;
    return this;
  }
  
  @Override
  public GroupQuery groupType(String type) {
    if (type == null) {
      throw new ActivitiIllegalArgumentException("Provided type is null");
    }
    this.type = type;
    return this;
  }
  
  @Override
  public GroupQuery groupMember(String userId) {
    if (userId == null) {
      throw new ActivitiIllegalArgumentException("Provided userId is null");
    }
    this.userId = userId;
    return this;
  }

  @Override
  public GroupQuery potentialStarter(String procDefId) {
    if (procDefId == null) {
      throw new ActivitiIllegalArgumentException("Provided processDefinitionId is null or empty");
    }
    this.procDefId = procDefId;
    return this;
    
  }
  
  //sorting ////////////////////////////////////////////////////////
  
  @Override
  public GroupQuery orderByGroupId() {
    return orderBy(GroupQueryProperty.GROUP_ID);
  }
  
  @Override
  public GroupQuery orderByGroupName() {
    return orderBy(GroupQueryProperty.NAME);
  }
  
  @Override
  public GroupQuery orderByGroupType() {
    return orderBy(GroupQueryProperty.TYPE);
  }
  
  //results ////////////////////////////////////////////////////////
  
  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return commandContext
      .getGroupIdentityManager()
      .findGroupCountByQueryCriteria(this);
  }
  
  @Override
  public List<Group> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    return commandContext
      .getGroupIdentityManager()
      .findGroupByQueryCriteria(this, page);
  }
  
  //getters ////////////////////////////////////////////////////////
  
  public String getId() {
    return id;
  }
  public String getName() {
    return name;
  }
  public String getNameLike() {
    return nameLike;
  }
  public String getType() {
    return type;
  }
  public String getUserId() {
    return userId;
  }

  
}
