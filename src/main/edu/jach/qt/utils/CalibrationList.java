package edu.jach.qt.utils;

/* Gemini imports */
import gemini.sp.*;

/* Standard imports */
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;

import orac.util.*;
import om.util.*;
import org.apache.log4j.Logger;

import edu.jach.qt.gui.*;


/* Miscellaneous imports */
import org.apache.log4j.Logger;
import org.apache.xerces.dom.*;
import org.apache.xml.serialize.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;  

/**
 * This class returns a <code>Hashtable</code> of calibrations.  Each entry in the
 * hashtable takes the form (String title, Integer id), where 
 * title is the title of the Observation and ID is its unique identifier.
 * Calibration entries are expected to be in the datase and belong to a
 * project called "CAL".  This project must be uniquye and ONLY contain
 * calibration observations.
 *
 * @author   $Author$
 * @version  $Revision$
 */

public class CalibrationList {

    private static final String OBSERVABILITY_DISABLED = "observability";
    private static final String REMAINING_DISABLED     = "remaining";
    private static final String ALLOCATION_DISABLED    = "allocation";
    private static final String ALL_DISABLED           = "all";
    public  static final String ROOT_ELEMENT_TAG = "SpMSBSummary";

    static Logger logger = Logger.getLogger(CalibrationList.class);

    /**
     * Constructor
     */
    private CalibrationList() {
    }

    /**
     * Get the list of calibration observations for a specified telescope.
     *
     * @param telescope     The name os the telescope.
     * @return              A <code>Hashtable</code> of observations.  If no
     *                      observations are found then there will be
     *                      zero entries in the table.
     */
    public static Hashtable getCalibrations(String telescope) {
	SpItem sp;
	Hashtable myCalibrations = new Hashtable();
	Document doc = new DocumentImpl();
	Element root = doc.createElement("MSBQuery");
	Element item;

	/* Construct the query */
	item = doc.createElement("disableconstraint");
	item.appendChild( doc.createTextNode(ALL_DISABLED) ); // Disables all constraints
	root.appendChild(item);

	/* 
	 * Add the telescope element - there should be unique a unique CAL project for
	 * each telescope
	 */
	item = doc.createElement("telescope");
	item.appendChild( doc.createTextNode(System.getProperty("telescope")) ); 
	root.appendChild(item);

	/* 
	 * The calibration project id MUST be of the form <TELESCOPE>CAL
	 * e.g. UKIRTCAL, JCMTCAL, GEMININCAL etc
	 */
	String calibrationProject = telescope.toUpperCase() + "CAL";
	item = doc.createElement("projectid");
	item.appendChild(doc.createTextNode(calibrationProject));
	root.appendChild(item);

	doc.appendChild(root);

	OutputFormat  fmt    = new OutputFormat(doc, "UTF-8", true);
	StringWriter  writer = new StringWriter();
	XMLSerializer serial = new XMLSerializer(writer, fmt);
	try {
	    serial.asDOMSerializer();
	    serial.serialize( doc.getDocumentElement() );
	} catch (IOException ioe) {return null;}

	/* Send the query to the database */
	String result = MsbClient.queryCalibration(writer.toString());
	if (result == null || result.equals("")) {
	    return null;
	}

	/*
	 * To allow us to parser the returned XML, create a temporary file
	 * to write the XML to, and then build it again using the document model
	 */
	try {
	    File tmpFile = File.createTempFile("calibration",".xml");
	    FileWriter fw = new FileWriter(tmpFile);
	    fw.write(result);
	    fw.close();

	    doc=null;

	    //Build the document factory and try to parse the results
	    DocumentBuilderFactory factory =
		DocumentBuilderFactory.newInstance();
	    
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    doc = builder.parse( tmpFile );

	    if (doc != null) {
		/*
		 * Since this only contains calibrations, loop through every node and
		 * get the title and identifier
		 */
		for (int node=0; node < XmlUtils.getSize( doc , ROOT_ELEMENT_TAG ); node++) {
		    item = XmlUtils.getElement( doc , ROOT_ELEMENT_TAG , node );
		    myCalibrations.put((String) XmlUtils.getValue(item, "title"),
				       new Integer (item.getAttribute("id")));
		}
	    }
	    else {
		logger.warn("No Calibration results returned");
	    }
	    tmpFile.delete();
	    
	} catch (SAXException sxe) {
	    Exception  x = sxe;
	    if (sxe.getException() != null)
		x = sxe.getException();
	    logger.error("SAX Error generated during parsing", x);
	    
	} catch(ParserConfigurationException pce) {
	    logger.error("ParseConfiguration Error generated during parsing", pce);
	} catch (IOException ioe) {
	    logger.error("IO Error generated attempting to build Document", ioe);
	}
	    
	// return the hopefully populated hashtable.  If no entries, we retirn
	// the hashtable anyway and rely on the caller to realise that it is of zero size
	return myCalibrations;
    }


}
