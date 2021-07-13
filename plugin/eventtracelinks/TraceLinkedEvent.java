package org.processmining.logqualityquantification.plugin.eventtracelinks;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;

public class TraceLinkedEvent {
	private XTrace trace;
	private XEvent event;

	public TraceLinkedEvent(XTrace trace, XEvent event) {
		this.setTrace(trace);
		this.setEvent(event);
	}

	public XTrace getTrace() {
		return trace;
	}

	public void setTrace(XTrace trace) {
		this.trace = trace;
	}

	public XEvent getEvent() {
		return event;
	}

	public void setEvent(XEvent event) {
		this.event = event;
	}
}


