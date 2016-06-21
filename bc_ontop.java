import ij.plugin.*;
import ij.plugin.frame.*;

// This plugin calls the Brightness/Contrast adjustment tool and makes 
// sure it appears in front of any image windows. 
// created by - Richard Zorger, University of Pennsylvania

public class bc_ontop {

	public static String method0() {
		ContrastAdjuster Adjust_contrast = new ContrastAdjuster();
		Adjust_contrast.run("bc");
		Adjust_contrast.setAlwaysOnTop(true);	
		return "";
	}
}