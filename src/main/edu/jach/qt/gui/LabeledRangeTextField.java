package edu.jach.qt.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import java.util.*;

/**
 * LabeldRangeTextField.java
 *
 *
 * Created: Tue Sep 25 15:55:43 2001
 *
 * @author <a href="mailto: mrippa@jach.hawaii.edu"Mathew Rippa</a>
 */

public class LabeledRangeTextField extends WidgetPanel 
  implements DocumentListener {

  private JTextField upperBound;
  private JTextField lowerBound;

  private JLabel     widgetLabel;
  private JLabel     upperLabel;
  private JLabel     lowerLabel;

  private String name;

  public LabeledRangeTextField(Hashtable ht, WidgetDataBag wdb, String text) {
    super(ht, wdb);

    widgetLabel = new JLabel(text + ": ", JLabel.LEADING);
    lowerLabel = new JLabel("Min: ",JLabel.TRAILING);
    upperLabel = new JLabel("Max: ",JLabel.TRAILING);


    upperBound = new JTextField();
    lowerBound = new JTextField();
    setup();
  }

  private void setup() {
    name = widgetLabel.getText().trim();
    GridLayout gl = new GridLayout(0,5);
    gl.setHgap(0);
    setForeground(Color.white);
    widgetLabel.setForeground(Color.black);

    setLayout(gl);
    add(widgetLabel);

    add(lowerLabel);
    add(lowerBound);
    lowerBound.getDocument().addDocumentListener(this);
    
    add(upperLabel);
    add(upperBound);
    upperBound.getDocument().addDocumentListener(this);
  }

  public String getName() {
    return abbreviate(name);
  }

  public String getUpperText() {
    return upperBound.getText();
  }

  public String getLowerText() {
    return lowerBound.getText();
  }

  public Vector getUpperList() {
    String tmpStr = getUpperText();
    Vector result = new Vector();
    StringTokenizer st = new StringTokenizer(tmpStr, ",");

    while (st.hasMoreTokens()) {
      String temp1 = st.nextToken();
      result.add(temp1);
    }
    return result;
  }
 
  public Vector getLowerList() {
    String tmpStr = getLowerText();
    Vector result = new Vector();
    StringTokenizer st = new StringTokenizer(tmpStr, ",");

    while (st.hasMoreTokens()) {
      String temp1 = st.nextToken();
      result.add(temp1);
    }
    return result;
  }
 
  /**
   * The <code>insertUpdate</code> adds the current text to the
   * WidgetDataBag.  All observers are notified.
   *
   * @param e a <code>DocumentEvent</code> value
   */
  public void insertUpdate(DocumentEvent e) {

    setAttribute(name.substring(0,name.length()-1), this);
  }

  /**
   * The <code>removeUpdate</code> adds the current text to the
   * WidgetDataBag.  All observers are notified.
   *
   * @param e a <code>DocumentEvent</code> value
   */
  public void removeUpdate(DocumentEvent e) {
    setAttribute(name.substring(0,name.length()-1), this);
  }

  /**
   * The <code>changedUpdate</code> method is not implemented.
   *
   * @param e a <code>DocumentEvent</code> value
   */
  public void changedUpdate(DocumentEvent e) {
      
  }

  
}
