package edu.jach.qt.app;

import edu.jach.qt.gui.QtFrame;
import java.awt.*;
import javax.swing.UIManager;
import edu.jach.qt.gui.WidgetDataBag;

/**
 * Describe class <code>QT</code> here.
 *
 * @author <a href="mailto:mrippa@jach.hawaii.edu">Mathew Rippa</a>
 * @version $Version$
 */
public class QT {

   boolean packFrame = false;
   WidgetDataBag wdb = new WidgetDataBag ();

   /**
    * Creates a new <code>QT</code> instance.
    *
    */
   public QT () {
      Querytool qt = new Querytool(wdb);
      QtFrame qtf = new QtFrame(wdb, qt);
      
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      Dimension frameSize = qtf.getSize();

      // Validate frames that have preset sizes
      // Pack frames that have useful preferred size info, e.g. from their layout
      if (packFrame) {
	 qtf.pack();
      }
      else {
	 qtf.validate();
      }

      //Center the window
      if (frameSize.height > screenSize.height) {
	 frameSize.height = screenSize.height;
      }
      if (frameSize.width > screenSize.width) {
	 frameSize.width = screenSize.width;
      }
      qtf.setLocation(22,20);
      qtf.setVisible(true);

   }
   
   /**
    * Describe <code>main</code> method here.
    *
    * @param args a <code>String[]</code> value
    */
   public static void main(String[] args) {
      try {
	 UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
      catch(Exception e) {
	 e.printStackTrace();
      }
      new QT();
   }
} // Omp

//$Log$
//Revision 1.2  2001/09/07 01:18:10  mrippa
//The QT now supports a query of the MSB server retrieving a MSB summaries.
//The summaries are displayed in a JTable which listens for double clicks
//on the rows, corresponding to the MSB ID for that summary.  The MSB is
//then translated and given to the OM for lower level processing.
//
//Revision 1.1.1.1  2001/08/28 02:53:45  mrippa
//Import of QT
//
//Revision 1.3  2001/07/27 19:49:05  mrippa
//More comments
//
