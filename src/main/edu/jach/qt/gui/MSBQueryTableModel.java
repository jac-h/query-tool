package edu.jach.qt.gui;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.xml.parsers.*;
import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.xml.sax.SAXException;  

/**
 * MSBQueryTableModel.java
 *
 *
 * Created: Tue Aug 28 16:49:16 2001
 *
 */

public class MSBQueryTableModel extends AbstractTableModel implements Runnable {

  static Logger logger = Logger.getLogger(MSBQueryTableModel.class);
  public static final String ROOT_ELEMENT_TAG = "SpMSBSummary";

  public static final String MSB_SUMMARY = System.getProperty("msbSummary")+"."+System.getProperty("user.name");
  public static final String MSB_SUMMARY_TEST = System.getProperty("msbSummaryTest");

  /*public static final String[] colNames ={
    "ProjectID",
    "SourceName",
    "Instrument",
    "Wavelength",
    "ExposureTime",
    "PI"
    };*/
   
  public static final String[] colNames ={
    "projectid",
    "title",
    "instrument",
    "waveband",
    "target",
    "ra",
    "coordstype",
    "ha",
    "timeest",
    "priority",
    "remaining",
    "obscount",
    "checksum",
    "msbid",
  };
   
  public static final Class[] colClasses = {
    String.class,	//0
    String.class,	//1
    String.class,	//2
    String.class,	//3
    String.class,	//4
    String.class,	//5
    String.class,	//6
    String.class,	//7
    Integer.class,	//8
    Integer.class,	//9
    Integer.class,	//10
    Integer.class,	//11
    String.class,	//12
    Integer.class,	//13
  };

  public static final int 
    PROJECTID           = 0,
    TITLE               = 1,
    INSTRUMENT          = 2,
    WAVEBAND		= 3,
    TARGET              = 4,
    RA		        = 5, 
    COORDSTYPE          = 6, 
    HA		        = 7,
    TIMEEST             = 8, //INT
    PRIORITY            = 9,  //INT
    REMAINING           = 10, //INT
    OBSCOUNT            = 11, //INT
    CHECKSUM            = 12,
    MSBID		= 13; //INT
      
  //DATA
  //DOM object to hold XML document contents
  protected Document doc;
  protected Element msbIndex;
  public Integer[] projectIds;
  boolean docIsNull;

  //used to hold a list of TableModelListeners
  protected java.util.List tableModelListeners = 
    new ArrayList();        

  public MSBQueryTableModel() {
    docIsNull = true;
    projectIds = new Integer[200];
  }

  public void run() {
    /**
       Constructor - create a DOM
    */

      System.out.println("In MSBQueryTableModel.run()");

    try {
      DocumentBuilderFactory factory =
	DocumentBuilderFactory.newInstance();
      //factory.setValidating(true);   
      //factory.setNamespaceAware(true);
	 
      DocumentBuilder builder = factory.newDocumentBuilder();
      doc = builder.parse( new File(MSB_SUMMARY));
      //doc = builder.parse( new File(MSB_SUMMARY_TEST));
      //System.out.println("doc: "+doc);
      docIsNull = false;


    } catch (SAXException sxe) {
      Exception  x = sxe;
      if (sxe.getException() != null)
	x = sxe.getException();
      //x.printStackTrace();
      logger.error("SAX Error generated during parsing", x);

    } catch(ParserConfigurationException pce) {
      logger.error("ParseConfiguration Error generated during parsing", pce);
      //pce.printStackTrace();
    } catch (IOException ioe) {
      // I/O error
      logger.error("IO Error generated attempting to build Document", ioe);
      //ioe.printStackTrace();
    }
      
      System.out.println("Firing event from MSBQueryTableModel.run()");

      fireTableChanged(null);
  }

  public Document getDoc() {
    return doc;
  }

  //
  // TableModel implementation
  //

  /**
     Return the number of columns for the model.

     @return    the number of columns in the model
  */
  public int getColumnCount() {
    return colClasses.length;
  }
  /**
     Return the number of persons in an XML document
 
     @return    the number or rows in the model
  */
  public int getRowCount() {
    if (docIsNull) return 0;
    return XmlUtils.getSize( doc , ROOT_ELEMENT_TAG );
  }

  /**
     Return an XML data given its location
 
     @param	    r   the row whose value is to be looked up
     @param	    c 	the column whose value is to be looked up
     @return	the value Object at the specified cell
  */
  public Object getValueAt(int r, int c) {

    //must get row first
    Element row = XmlUtils.getElement( doc , ROOT_ELEMENT_TAG , r );
    //Element indexElement = msbDoc.createElement("index");

    projectIds[r] = new Integer( row.getAttribute("id"));

    //must get value for column in this row
    return XmlUtils.getValue( row , colNames[c] );
  }

  public Integer getSpSummaryId(int row) {
    return projectIds[row];
  }

  /**
     Return the name of column for the table.
 
     @param	    c   the index of column
     @return    the name of the column
  */
  public String getColumnName(int c) {
    return colNames[ c ];
  }
  /**
     Return column class
 
     @parm      c the index of column
     @return    the common ancestor class of the object values in the model.
  */
  public Class getColumnClass(int c) {
    return colClasses[ c ];
  }

  /**
     Return false - table is not editable
 
     @param	    r	the row whose value is to be looked up
     @param	    c	the column whose value is to be looked up
     @return	true if the cell is editable.
  */
  public boolean isCellEditable(int r, int c) {
    return false;
  }

  /**
     This method is not implemented, because the table is not editable.
 
     @param	    value		 the new value
     @param	    r	 the row whose value is to be changed
     @param	    c 	 the column whose value is to be changed
  */
  public void setValueAt(Object value, int r, int c) {
  }

}// MSBQueryTableModel
