package org.activiti.engine.impl.event.logger;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.impl.event.logger.handler.EventLoggerEventHandler;
import org.activiti.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public abstract class AbstractEventFlusher implements EventFlusher {
	
	protected List<EventLoggerEventHandler> eventHandlers = new ArrayList<EventLoggerEventHandler>();
	
	@Override
	public void closed(CommandContext commandContext) {
		// Not interested in closed
	}
	
	@Override
    public List<EventLoggerEventHandler> getEventHandlers() {
		return eventHandlers;
	}

	@Override
    public void setEventHandlers(List<EventLoggerEventHandler> eventHandlers) {
		this.eventHandlers = eventHandlers;
	}
	
	@Override
    public void addEventHandler(EventLoggerEventHandler databaseEventLoggerEventHandler) {
		eventHandlers.add(databaseEventLoggerEventHandler);
	}
	
}
