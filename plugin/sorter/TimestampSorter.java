package org.processmining.logqualityquantification.plugin.sorter;

import java.util.Comparator;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.processmining.logqualityquantification.plugin.eventtracelinks.TraceLinkedEvent;

public class TimestampSorter implements Comparator<TraceLinkedEvent>
{
    public int compare(TraceLinkedEvent tle1, TraceLinkedEvent tle2)
    {
        return XTimeExtension.instance().extractTimestamp(tle1.getEvent()).compareTo(XTimeExtension.instance().extractTimestamp(tle2.getEvent()));
    }
}
