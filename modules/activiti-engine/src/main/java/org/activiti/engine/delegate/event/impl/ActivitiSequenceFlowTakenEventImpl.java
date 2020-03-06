package org.activiti.engine.delegate.event.impl;

import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.ActivitiSequenceFlowTakenEvent;

/**
 * @author Joram Barrez
 */
public class ActivitiSequenceFlowTakenEventImpl extends ActivitiEventImpl implements ActivitiSequenceFlowTakenEvent {
	
	protected String id;
	protected String sourceActivityId;
	protected String sourceActivityName;
	protected String sourceActivityType;
	protected String sourceActivityBehaviorClass;
	protected String targetActivityId;
	protected String targetActivityName;
	protected String targetActivityType;
	protected String targetActivityBehaviorClass;
	
	public ActivitiSequenceFlowTakenEventImpl(ActivitiEventType type) {
	  super(type);
  }

	@Override
    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
    public String getSourceActivityId() {
		return sourceActivityId;
	}

	public void setSourceActivityId(String sourceActivityId) {
		this.sourceActivityId = sourceActivityId;
	}

	@Override
    public String getSourceActivityName() {
		return sourceActivityName;
	}

	public void setSourceActivityName(String sourceActivityName) {
		this.sourceActivityName = sourceActivityName;
	}

	@Override
    public String getSourceActivityType() {
		return sourceActivityType;
	}

	public void setSourceActivityType(String sourceActivityType) {
		this.sourceActivityType = sourceActivityType;
	}

	@Override
    public String getSourceActivityBehaviorClass() {
		return sourceActivityBehaviorClass;
	}

	public void setSourceActivityBehaviorClass(String sourceActivityBehaviorClass) {
		this.sourceActivityBehaviorClass = sourceActivityBehaviorClass;
	}

	@Override
    public String getTargetActivityId() {
		return targetActivityId;
	}

	public void setTargetActivityId(String targetActivityId) {
		this.targetActivityId = targetActivityId;
	}

	@Override
    public String getTargetActivityName() {
		return targetActivityName;
	}

	public void setTargetActivityName(String targetActivityName) {
		this.targetActivityName = targetActivityName;
	}

	@Override
    public String getTargetActivityType() {
		return targetActivityType;
	}

	public void setTargetActivityType(String targetActivityType) {
		this.targetActivityType = targetActivityType;
	}

	@Override
    public String getTargetActivityBehaviorClass() {
		return targetActivityBehaviorClass;
	}

	public void setTargetActivityBehaviorClass(String targetActivityBehaviorClass) {
		this.targetActivityBehaviorClass = targetActivityBehaviorClass;
	}
	
}
