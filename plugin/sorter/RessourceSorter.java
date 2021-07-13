package org.processmining.logqualityquantification.plugin.sorter;

import java.util.Comparator;

import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;

public class RessourceSorter implements Comparator<TraceLinkedEvent>
{
    public int compare(TraceLinkedEvent tle1, TraceLinkedEvent tle2)
    {
		return XOrganizationalExtension.instance().extractResource((tle1.getEvent())).compareTo(XOrganizationalExtension.instance().extractResource(tle2.getEvent()));
	}
}

