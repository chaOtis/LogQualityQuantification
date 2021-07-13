package org.processmining.logqualityquantification.plugin;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;

public class LQ_Plugin {

	@Plugin(
			name = "Log Quality Quantification", 
			parameterLabels = {"Original Log"}, 
		    returnLabels = { }, 
		    returnTypes = { }, 
		    help = YourHelp.TEXT,
		    mostSignificantResult = -1)
	@UITopiaVariant(affiliation = "FIM Research Center, University of Bayreuth", author = "D.A. Fischer", email = "dominik.fischer@fim-rc.de")
	public void createQIEL(UIPluginContext context, XLog log)  {
		QualityInformedEventLog qiel = new QualityInformedEventLog(context, log);
	}
		
	/*@Plugin(
			name = "Log Quality Quantification and Comparison", 
			parameterLabels = { "Original Log", "Repaired Log"}, 
		    returnLabels = {"Log Quality Quantification and Comparison"}, 
		    returnTypes = { FinalPanel.class }, 
		    help = YourHelp.TEXT)
	@UITopiaVariant(affiliation = "QUT", author = "D.A. Fischer", email = "dominik.fischer@qut.edu.au")
	public FinalPanel twoLog(UIPluginContext context, XLog log1, XLog log2) throws IllegalAccessException,
			InstantiationException {
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(30);
		context.getProgress().setIndeterminate(false);
		return new FinalPanel(new LogQualityPanel(log1, context), new LogQualityPanel(log2, context));
	}*/
}


