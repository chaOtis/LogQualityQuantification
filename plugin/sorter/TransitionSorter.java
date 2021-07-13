package org.processmining.logqualityquantification.plugin.sorter;

import java.util.Comparator;

import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;

public class TransitionSorter implements Comparator<TraceLinkedEvent>
{
    public int compare(TraceLinkedEvent tle1, TraceLinkedEvent tle2)
    {
    	if (XLifecycleExtension.instance().extractTransition(tle1.getEvent()) != null && XLifecycleExtension.instance().extractTransition(tle2.getEvent()) != null) {
    		return XLifecycleExtension.instance().extractTransition(tle1.getEvent()).compareTo(XLifecycleExtension.instance().extractTransition(tle2.getEvent()));
    	}
		return 0;
	}
}

