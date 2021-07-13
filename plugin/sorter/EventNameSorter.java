package org.processmining.logqualityquantification.plugin.sorter;

import java.util.Comparator;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;

public class EventNameSorter implements Comparator<TraceLinkedEvent>
{
    public int compare(TraceLinkedEvent tle1, TraceLinkedEvent tle2)
    {
        return XConceptExtension.instance().extractName(tle1.getEvent()).compareTo(XConceptExtension.instance().extractName(tle2.getEvent()));
    }
}

