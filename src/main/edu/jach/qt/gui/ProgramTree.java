package edu.jach.qt.gui;


/* Gemini imports */
import gemini.sp.*;
import gemini.sp.obsComp.*;

/* JSKY imports */
import jsky.app.ot.*;

/* ORAC imports */
import orac.jcmt.inst.*;
import orac.jcmt.iter.*;
import orac.jcmt.obsComp.*;
import orac.ukirt.inst.*;
import orac.ukirt.iter.*;
import orac.util.*;

/* ORAC-OM imports */
import om.console.*;
import om.util.*;

/* QT imports */
import edu.jach.qt.utils.*;

/* Standard imports */
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DragSourceContext;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.tree.*;

/* Miscellaneous imports */
import ocs.utils.*;
import org.apache.log4j.Logger;


/**
   final public class programTree is a panel to select
   an observation from a List object. 

   @version 1.0 1st June 1999
   @author M.Tan@roe.ac.uk, modified by Mathew Rippa
*/
final public class ProgramTree extends JPanel implements 
    TreeSelectionListener, 
    ActionListener,
    KeyListener,
    DragSourceListener,
    DragGestureListener, 
    DropTargetListener {

  static Logger logger = Logger.getLogger(ProgramTree.class);

  public static final String BIN_IMAGE = System.getProperty("binImage");
  public static final String BIN_SEL_IMAGE = System.getProperty("binImage");

  private GridBagConstraints		gbc;
  private JButton			run;
  private JButton                       xpand;
  private JTree				tree;
  private static JList			        obsList;
  private DefaultListModel		model;
  private JScrollPane			scrollPane = new JScrollPane();;
  private SpItem			_spItem;
  private DefaultMutableTreeNode	root;
  private DefaultTreeModel		treeModel;
  private TreeViewer                    tv=null;
  private TreePath			path;
  private String			projectID, checkSum;
//   private SequenceManager		scm;
    private DropTarget                  dropTarget=null;
    private DragSource                  dragSource=null;
    private TrashCan                    trash=null;
    public static  SpItem          selectedItem=null;
    public static  SpItem          obsToDefer;
    private SpItem                 instrumentContext;
    private Vector                 targetContext;
    private final String           editText = "Edit Attribute...";
    private final String           scaleText = "Scale Exposure Times...";
    private String                 rescaleText = "Re-do Scale Exposure Times";

  /** public programTree() is the constructor. The class
      has only one constructor so far.  a few thing are done during
      the construction. They are mainly about adding a run button and
      setting up listeners.
      
      @param  none
      @return none
      @throws none 
  */
  public ProgramTree()  {

//     scm = SequenceManager.getHandle();

    // Ensure nothing is selected 
    selectedItem = null;

    Border border=BorderFactory.createMatteBorder(2, 2, 2, 2, Color.white);
    setBorder(new TitledBorder(border, "Fetched Science Program (SP)", 
			       0, 0, new Font("Roman",Font.BOLD,12),Color.black));
    setLayout(new BorderLayout() );

    GridBagLayout gbl = new GridBagLayout();
    setLayout(gbl);
    gbc = new GridBagConstraints();

    run=new JButton("Send for Execution");
    run.setMargin(new Insets(5,10,5,10));
    if (TelescopeDataPanel.DRAMA_ENABLED) {
	run.setEnabled(true);
    }
    else {
	run.setEnabled(false);
    }
    run.addActionListener(this);

    xpand = new JButton("Expand Observation");
    xpand.setMargin(new Insets(5,10,5,10));
    xpand.addActionListener(this);

    dropTarget=new DropTarget();
    try{
	dropTarget.addDropTargetListener(this);
    }catch(TooManyListenersException tmle){
	logger.error("Too many drop target listeners", tmle);
    }

    trash = new TrashCan();
    trash.setDropTarget(dropTarget);

    dragSource = new DragSource();

    // Create a popup menu 
    popup = new JPopupMenu();
    edit = new JMenuItem (editText);
    edit.addActionListener(this);
    popup.add (edit);
    scale = new JMenuItem (scaleText);
    scale.addActionListener(this);
    popup.add (scale);
    scaleAgain = new JMenuItem (rescaleText);
    scaleAgain.addActionListener(this);
    scaleAgain.setEnabled(false);
    popup.add (scaleAgain);

    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.weightx = 100;
    gbc.weighty = 100;
    gbc.insets.left = 10;
    gbc.insets.right = 0;
    add(trash, gbc, 1, 1, 1, 1);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 100;
    gbc.weighty = 0;
    gbc.insets.left = 0;
    gbc.insets.right = 0;
    add(run, gbc, 0, 1, 1, 1);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 100;
    gbc.weighty = 0;
    gbc.insets.left = 0;
    gbc.insets.right = 0;
    gbc.insets.bottom = 50;
    add(xpand, gbc, 0, 2, 1, 1);
  }

    /**
     * Set the <code>projectID</code> to a specified value.
     * @param projectID  The value to set.
     */
  public void setProjectID(String projectID) {
    this.projectID = projectID;
  }

    /**
     * Set the <code>checkSum</code> to a specified value.
     * @param checksum  The value to set.
     */
  public void setChecksum(String checksum) {
    this.checkSum = checksum;
  }

    /**
     * Set the "Send for Execution" to (dis)abled.
     * @param  flag  <code>true</code> to enable execution.
     */
    public void setExecutable (boolean flag) {
	run.setEnabled(flag);
    }

    /**
     * Set the Trash Can image.
     * @param label  The <code>JLabel</code> with which to associate the image.
     * @exception Exception is unabe to set the image.
     */
  public void setImage(JLabel label) throws Exception {
    URL url = new URL("file://"+BIN_IMAGE);
    if(url != null) {
      label.setIcon(new ImageIcon(url));
    }
    else {
      label.setIcon(new ImageIcon(ProgramTree.class.getResource("file://"+BIN_IMAGE)));
    }
  }

  /**
   * Add a compnent to the <code>GridBagConstraints</code>
   *
   * @param c a <code>Component</code> value
   * @param gbc a <code>GridBagConstraints</code> value
   * @param x an <code>int</code> value
   * @param y an <code>int</code> value
   * @param w an <code>int</code> value
   * @param h an <code>int</code> value
   */
  public void add(Component c, GridBagConstraints gbc, 
		  int x, int y, int w, int h) {
    gbc.gridx = x;
    gbc.gridy = y;
    gbc.gridwidth = w;
    gbc.gridheight = h;
    add(c, gbc);      
  }

  
  /** public void actionPerformed(ActionEvent evt) is a public method
      to react button actions. The reaction is mainly about to start a
      SGML translation, then a "remote" frame to form a sequence console.
      
      @param ActionEvent
      @return  none
      @throws none
      
  */
  public void actionPerformed (ActionEvent evt) {
    Object source = evt.getSource();
    if (source == run) {
	doExecute();
    }
    else if (source == xpand) {
	SpItem itemToXpand;
	if (selectedItem == null && DeferredProgramList.currentItem == null) {
	    return;
	}
	else if (selectedItem == null) {
	    itemToXpand = DeferredProgramList.currentItem;
	}
	else {
	    itemToXpand = selectedItem;
	}

	if (tv == null) {
	    tv = new TreeViewer(itemToXpand);
	}
	else {
	    tv.update(itemToXpand);
	}
    }

    if (source instanceof JMenuItem) {
	JMenuItem thisItem = (JMenuItem) source;
	if ( thisItem.getText().equals(editText) ) {
	    editAttributes();
	} 
	else if ( thisItem.getText().equals(scaleText) ) {
	    scaleAttributes();
	} 
	else if ( thisItem.getText().equals(rescaleText) ) {
	    rescaleAttributes();   
	}
    }
  }

    public void doExecute() {
	SpItem item = null;
	boolean isDeferred = false;
	boolean failed = false;

	if (selectedItem == null) {
	    isDeferred =  true;
	    item = DeferredProgramList.currentItem;
	}
	run.setEnabled(false);
	if (System.getProperty("telescope").equalsIgnoreCase("ukirt")) {
	    try {
		ExecuteUKIRT execute = new ExecuteUKIRT();
		Thread t = new Thread(execute);
		t.start();
		t.join();
		File failFile = new File ("/ukirtdata/orac_data/deferred/.failure");
		if (failFile.exists()) {
		    failed = true;
		}
		if (!isDeferred && !failed) {
		    model.remove(obsList.getSelectedIndex());
		}
		else if (!failed) {
		    DeferredProgramList.markThisObservationAsDone(item);
		}
	
		if ( model.isEmpty() && TelescopeDataPanel.DRAMA_ENABLED) {
		    int mark = JOptionPane.showConfirmDialog(this, "Mark the MSB with \n"+
							     "Project ID: "+projectID+"\n"+
							     "CheckSum: "+checkSum+"\n"+
							     "as done?",
							     "MSB complete",
							     JOptionPane.YES_NO_OPTION);
		    if (mark == JOptionPane.YES_OPTION) {
			MsbClient.doneMSB(projectID, checkSum);
			// Since the MSBID has changed, redo the search...
			InfoPanel.searchButton.doClick();
		    }
		} // end of if ()

		run.setEnabled(true);
	    }
	    catch (Exception e) {
		logger.error("Failed to execute");
		run.setEnabled(true);
		return;
	    }
	}
	else if (System.getProperty("telescope").equalsIgnoreCase("jcmt")) {
	    ExecuteJCMT execute;
	    try {
		if (isDeferred) {
		    execute = new ExecuteJCMT(item);
		}
		else {
		    execute = new ExecuteJCMT(_spItem);
		}
		Thread t = new Thread(execute);
		t.start();
		t.join();
		File failFile = new File ("/jcmtdata/orac_data/deferred/.failure");
		if (failFile.exists()) {
		    failed = true;
		}
		if (!isDeferred && !failed) {
		    model.clear();
		    _spItem = null;
		    selectedItem = null;
		}
		else if (!failed) {
		    DeferredProgramList.markThisObservationAsDone(item);
		}
	
		run.setEnabled(true);
	    }
	    catch (Exception e) {
		logger.error("Failed to execute");
		run.setEnabled(true);
		return;
	    }
	}
    }
  

  /**
     public void addTree(String title,SpItem sp) is a public method
     to set up a JTree GUI bit for a science program object in the panel
     and to set up a listener too
     
     @param String title and SpItem sp
     @return  none
     @throws none
     @deprecated  Replaced by {@link #addList(SpItem)}
     
  */
  public void addTree(SpItem sp)
  {
    _spItem=sp;

    // Create data for the tree
    // root= new DefaultMutableTreeNode(sp);

    getItems(sp, root);
            
    // Create a new tree control
    treeModel = new DefaultTreeModel(root);
    tree = new JTree(treeModel);
      

    MyTreeCellRenderer tcr = new MyTreeCellRenderer();
    // Tell the tree it is being rendered by our application
    tree.setCellRenderer(tcr);
    tree.addTreeSelectionListener(this);
    tree.addKeyListener(this);

    // Add the listbox to a scrolling pane
    scrollPane.getViewport().removeAll();
    scrollPane.getViewport().add(tree);
    scrollPane.getViewport().setOpaque(false);

    gbc.fill = GridBagConstraints.BOTH;
    //gbc.anchor = GridBagConstraints.EAST;
    gbc.insets.bottom = 5;
    gbc.insets.left = 10;
    gbc.insets.right = 5;
    gbc.weightx = 100;
    gbc.weighty = 100;
    add(scrollPane, gbc, 0, 0, 2, 1);
      
    this.repaint();
    this.validate();
  }

    /**
     * Set up the List GUI and populate it with the results of a query.
     * @param sp  The list of obervations in the MSB.
     */
  public void addList(SpItem sp) {

      if (sp == null) {
	  obsList = new JList();
      }
      else {
	  _spItem = sp;

	  getContext(sp);
	  model = new DefaultListModel();

	  Vector obsVector =  SpTreeMan.findAllItems(sp, "gemini.sp.SpObs");
	  
	  Enumeration e = obsVector.elements();
	  while (e.hasMoreElements() ) {
	      model.addElement(e.nextElement());
	  } // end of while ()

	  obsList = new JList(model);
	  obsList.setCellRenderer(new ObsListCellRenderer());
	  MouseListener ml = new MouseAdapter()
	      {
		  public void mouseClicked(MouseEvent e)
		  {
		      if (e.getClickCount() == 2) {
			  doExecute();
		      }
		      else if (e.getClickCount() == 1 && 
			       (e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK ) {
			  if (selectedItem != obsList.getSelectedValue() ) {
			      // Select the new item
			      selectedItem = (SpItem) obsList.getSelectedValue();
			      DeferredProgramList.clearSelection();
			  }
			  else if (e.getClickCount() == 1)
			      {
				  if (selectedItem != obsList.getSelectedValue() ) {
				      // Select the new item
				      selectedItem = (SpItem) obsList.getSelectedValue();
				      DeferredProgramList.clearSelection();
				  }
				  else {
				      obsList.clearSelection();
				      selectedItem = null;
				  }
			      }
		      }
		  }

		  public void mousePressed(MouseEvent e) {
		      DeferredProgramList.clearSelection();
		  }
	      };
	  obsList.addMouseListener(ml);
	  MouseListener popupListener = new PopupListener();
	  obsList.addMouseListener(popupListener);
	  obsList.setSelectedIndex(0);
	  selectedItem = (SpItem) obsList.getSelectedValue();

	  dragSource.createDefaultDragGestureRecognizer(obsList,
							DnDConstants.ACTION_MOVE,
							this);
      }
      // Add the listbox to a scrolling pane
      scrollPane.getViewport().removeAll();
      scrollPane.getViewport().add(obsList);
      scrollPane.getViewport().setOpaque(false);

      gbc.fill = GridBagConstraints.BOTH;
      //gbc.anchor = GridBagConstraints.EAST;
      gbc.insets.bottom = 5;
      gbc.insets.left = 10;
      gbc.insets.right = 5;
      gbc.weightx = 100;
      gbc.weighty = 100;
      add(scrollPane, gbc, 0, 0, 2, 1);
    
  }

  //public MsbNode getMsbNode() {
  //   return myObs;
  // }

    /**
     * Clear the selection from the Prgram Tree List.
     */
    public static void clearSelection() {
	obsList.clearSelection();
	selectedItem = null;
    }

    /**
     * Get the current <code>JTree</code>.
     * @return The current tree structure.
     * @deprecated - this class now implements a list, not a tree. Not Replaced
  public JTree getTree() {
    return tree;
  }
  
  /**
     public void removeTree( ) is a public method
     to remove a JTree GUI bit for a science program object in the panel
     and to set up a listener too
      
     @param none
     @return  none
     @throws none
     @deprecated Not replaced.
      
  */
  public void removeTree()
  {
    this.remove(scrollPane);
  }
  

  /**
     public void valueChanged( TreeSelectionEvent event) is a public method
     to handle tree item selections
     
     @param TreeSelectionEvent event
     @return  none
     @throws none
     @deprecated Not replaced.
     
  */
  public void valueChanged(TreeSelectionEvent event)
  {
    if( event.getSource() == tree )
      {
	// Display the full selection path
	path = tree.getSelectionPath();

	// The next section is with a view to possible
	// exposure time changes. Don't use until we know want we want
	// for sure.
	// 	  if(path.getLastPathComponent().toString().length()>14) {
	// 	    if(path.getLastPathComponent().toString().substring(0,14).equals("ot_ukirt.inst.")) {
	// 	      new newExposureTime(_spItem);
	// 	    }
	// 	  }
      }
  }

    /**
     * Implementation opf <code>KeyListener</code> interface.
     * If the delete key is pressed, removes the currently selected item.
     */
  public void keyPressed(KeyEvent e) {
    if( (e.getKeyCode() == KeyEvent.VK_DELETE))
      removeCurrentNode();
      
  }

    /**
     * Implementation of <code>KeyListener</code> interface.
     */
  public void keyReleased(KeyEvent e) { }

    /**
     * Implementation of <code>KeyListener</code> interface.
     */
  public void keyTyped(KeyEvent e) { }

  /**
   * Remove the currently selected node. 
   */
    public void removeCurrentNode() {

	SpObs item = (SpObs)obsList.getSelectedValue();

 	Vector obsV = SpTreeMan.findAllItems(_spItem, "gemini.sp.SpObs");
	int i;
	SpObs [] obsToDelete = {(SpObs)obsV.elementAt(obsList.getSelectedIndex())};
	try {
	    if ( item != null && SpTreeMan.evalExtract(obsToDelete) == true) {
		SpTreeMan.extract(obsToDelete);
		((DefaultListModel)obsList.getModel()).removeElementAt(obsList.getSelectedIndex());
	    }
	    else if (item == null) {
		JOptionPane.showMessageDialog(null,
					      "No Observation to remove",
					      "Message", JOptionPane.INFORMATION_MESSAGE);
		return;
	    }
	    else {
		JOptionPane.showMessageDialog(null,
					      "Encountered a problem deleting this observation",
					      "Message", JOptionPane.WARNING_MESSAGE);
	    }
	}
	catch (Exception e) {
	    logger.error ("Exception encountered while deleting observation", e);
	}
    }
   
  /**
   * public void getItems (SpItem spItem,DefaultMutableTreeNode node)
   * is a public method to add ALL the items of a sp object into the
   * JTree *recursively*.
   *   
   *   @param SpItem spItem,DefaultMutableTreeNode node
   *   @return  none
   *   @throws none
      
  */
  private void getItems (SpItem spItem,DefaultMutableTreeNode node)
  {
    Enumeration children = spItem.children();
    while (children.hasMoreElements())
      {
	SpItem  child = (SpItem) children.nextElement();
	  
	DefaultMutableTreeNode temp
	  = new DefaultMutableTreeNode(child);
	  
	node.add(temp);
	getItems(child,temp);
      }
  }
  
  
  /** 
      public void findItems (SpItem spItem,DefaultMutableTreeNode node)
      is a public method to find a named item in the SpItem list.
      
      @param SpItem spItem, String name
      @return  SpItem
      @throws none
      
  */
  private SpItem findItem (SpItem spItem, String name) {
    int index = 0;
    Enumeration children = spItem.children();
    SpItem tmpItem = null;
    while (children.hasMoreElements()) {
      SpItem  child = (SpItem) children.nextElement();
      if(child.toString().equals(name))
	return child;
      tmpItem = findItem(child,name);
      if(tmpItem != null)
	return tmpItem;
    }
    return null;
  }

    /**
     * private void disableRun()
     *
     * Disable the run option whilst other things are happening
     *
     **/
    private void disableRun() {
	run.setEnabled(false);
	run.setForeground(Color.white);
    }


    /**
     * private void enableRun()
     *
     * Enable the run option once other things have stopped
     *
     **/
    private void enableRun() {
	run.setEnabled(true);
	run.setForeground(Color.black);
    }


    /**
     * private void editAttributes()
     *
     * Invokes the attribute editor on the current item, as long as that
     * item is an observation.
     **/
    private void editAttributes() {
	

	// Recheck that this is an observation
	if (selectedItem.type()==SpType.OBSERVATION) {

	    disableRun();

	    SpObs observation = (SpObs) selectedItem;

	    if (!observation.equals(null)) {
		new AttributeEditor(observation, new javax.swing.JFrame(), true).show();
	    } 
	    else {
		JOptionPane.showMessageDialog(null,
					      "Current selection is not an observation.",
					      "Not an Obs!",
					      JOptionPane.INFORMATION_MESSAGE);
	    }
	    enableRun();
	}
    }
 
    /**
     * private void scaleAttributes()
     *
     * Invokes the attribute scaler on the current item, as long as that
     * item is an observation.
     **/
    private void scaleAttributes() {
	if (selectedItem == null || selectedItem.type() != SpType.OBSERVATION ) {
	    return;
	}
	
	disableRun();
	    
	SpObs observation = (SpObs) selectedItem;
	if (!observation.equals(null)) {
	    new AttributeEditor(observation, new javax.swing.JFrame(), true,
				"EXPTIME",
				haveScaled.contains(observation),
				lastScaleFactor(),
				false).show();
	    double sf = AttributeEditor.scaleFactorUsed();
	    if (sf > 0) {
		haveScaled.addElement(observation);
		scaleFactors.addElement(new Double(sf));
		scaleAgain.setEnabled(true);
		rescaleText = "Re-do Scale Exposure Times (x" + sf + ")";
		scaleAgain.setText(rescaleText);
	    }
	} 
	else {
	    JOptionPane.showMessageDialog(null,
					  "Current selection is not an observation.",
					  "Not an Obs!",
					  JOptionPane.INFORMATION_MESSAGE);
	}
	enableRun();
    }
    
  
    /**
     * private void rescaleAttributes()
     *
     * Reinvokes the attribute scaler on the current item, as long as that
     * item is an observation.
     **/
    private void rescaleAttributes() {
	if (selectedItem == null || selectedItem.type() != SpType.OBSERVATION ) {
	    return;
	}
	
	disableRun();
	
	SpObs observation = (SpObs) selectedItem;
	if (!observation.equals(null)) {
	    new AttributeEditor(observation, new javax.swing.JFrame(), true,
				"EXPTIME",
				haveScaled.contains(observation),
				lastScaleFactor(),
				true).show();
	    double sf = AttributeEditor.scaleFactorUsed();
	    if (sf > 0) {
		haveScaled.addElement(observation);
		scaleFactors.addElement(new Double(sf));
	    }
	} 
	else {
	    JOptionPane.showMessageDialog(null,
					  "Current selection is not an observation.",
					  "Not an Obs!",
					  JOptionPane.INFORMATION_MESSAGE);
	}
	enableRun();
    }

    private Double lastScaleFactor() {
	if (scaleFactors.size() == 0) {
	    if (AttributeEditor.scaleFactorUsed() > 0) {
		return new Double(AttributeEditor.scaleFactorUsed());
	    } 
	    else {
		return new Double(1.0); 
	    }
	} 
	else {
	    return (Double)scaleFactors.elementAt(scaleFactors.size()-1);
	}
    }

    private void getContext(SpItem item) {
	Vector obs  = SpTreeMan.findAllItems(item, "gemini.sp.SpObs");
	instrumentContext = SpTreeMan.findInstrument((SpObs)obs.firstElement());
	targetContext     = SpTreeMan.findAllItems(item, "gemini.sp.obsComp.SpTelescopeObsComp");
   }

    /**
     * Convert eacg observation in an SpMSB to a standalone thing.
     * @param xmlString  The SpProg as an XML string.
     * @return           The translated SpProg, or the original input on failure.
     */
    public static SpItem convertObs(SpItem item) {
	/*
	 * Get all of the observation, instrument and target fields
	 */
	SpItem _item = item;
	SpItem msb = ((SpItem)SpTreeMan.findAllItems(_item, "gemini.sp.SpObs").firstElement()).parent();
	Vector obs  = SpTreeMan.findAllItems(_item, "gemini.sp.SpObs");
	Vector targ = SpTreeMan.findAllItems(_item, "gemini.sp.obsComp.SpTelescopeObsComp");
	Vector iter = SpTreeMan.findAllItems(msb, "gemini.sp.iter.SpIterFolder");
// 	SpObsContextItem msb  = (SpObsContextItem)((SpItem)obs.firstElement()).parent();
	if (msb == null) {
	    logger.warn("Current Tree does not seem to contain an observation context!");
	    return item;
	}
	SpItem inst = SpTreeMan.findInstrument((SpObs)obs.firstElement());
	if (inst == null) {
	    logger.warn("Current Tree does not seem to contain an instrument!");
	   return item;
	}
	
	SpItem localInst;
	SpItem localTarget;
	SpInsertData spid;
	Object [] objArray = obs.toArray();
	SpObs  [] newObs = new SpObs [obs.size()];
	for (int i=0; i<obs.size(); i++) {
	    newObs[i] = (SpObs)objArray[i];
	}
	SpItem [] iterator = new SpItem [iter.size()];
	objArray = iter.toArray();
	for (int i=0; i<iter.size(); i++) {
	    iterator[i] = (SpItem)objArray[i];
	}
	
       	SpTreeMan.extract((SpItem []) newObs);
       	SpTreeMan.extract((SpItem) msb);
       	SpTreeMan.extract(iterator);
	for (int i=0; i<obs.size(); i++) {
	    if (SpTreeMan.findInstrumentInContext((SpObs)newObs[i]) == null) {
		/*
		 * The current observation does not contain an instrument
		 * so add the already found one.
		 */
		spid = SpTreeMan.evalInsertInside(inst, (SpObs)newObs[i]);
		SpTreeMan.insert(spid);
	    }
	    if (SpTreeMan.findTargetListInContext((SpObs)newObs[i]) == null) {
		/*
		 * The current observation does not contain a target
		 * so add the already found one.
		 */
		spid = SpTreeMan.evalInsertInside((SpItem)targ.firstElement(), (SpObs)newObs[i]);
		SpTreeMan.insert(spid);
	    }
	    /*
	     * Now we have updated all of the obs, try and replace the obs in the tree
	     */
	    if (SpTreeMan.evalExtract(item) == true) {
		spid = SpTreeMan.evalInsertInside(newObs[i], (SpItem)msb);
		SpTreeMan.insert(spid);
	    }
	}
 	System.exit(0);
	
	return item;
    }
  

    class PopupListener extends MouseAdapter {

	public void mousePressed (MouseEvent e) {

	    // If this was not the right button just return immediately.
	    if (! e.isPopupTrigger() || selectedItem == null) {
		return;
	    }
	    
	    // If this is an observation then show the popup
	    if (selectedItem.type()==SpType.OBSERVATION) {
		popup.show (e.getComponent(), e.getX(), e.getY());
	    }   
	}	
    }    

    /**
     * Implementation of <code>DropTargetListener</code> Interface 
     * @param evt  <code>DropTargetDragEvent</code> event
     */
    public void dragEnter(DropTargetDragEvent evt){
    }
  
    /**
     * Implementation of <code>DropTargetListener</code> Interface 
     * @param evt  <code>DropTargetEvent</code> event
     */
    public void dragExit(DropTargetEvent evt){
    }

    /**
     * Implementation of <code>DropTargetListener</code> Interface 
     * @param evt  <code>DropTargetDragEvent</code> event
     */
    public void dragOver(DropTargetDragEvent evt){
    }

    /**
     * Implementation of <code>DropTargetListener</code> Interface 
     * @param evt  <code>DropTargetDropEvent</code> event
     */
    public void drop(DropTargetDropEvent evt){
	SpObs itemForDrop;
	if (selectedItem != null) {
	    itemForDrop = (SpObs)selectedItem;
	}
	else {
	    itemForDrop = (SpObs)DeferredProgramList.currentItem;
	}

	if (itemForDrop != null && !itemForDrop.isOptional()) {
	    JOptionPane.showMessageDialog(null,
					  "Can not delete a mandatory observation!"
					  );
	    return;
	}
	
	evt.acceptDrop(DnDConstants.ACTION_MOVE);
	evt.getDropTargetContext().dropComplete(true);
	return;
    }

    /**
     * Implementation of <code>DropTargetListener</code> Interface 
     * @param evt  <code>DropTargetDragEvent</code> event
     */
    public void dropActionChanged(DropTargetDragEvent evt){
    }

    /**
     * Implementation of <code>DragGestureListener</code> Interface
     * @param event  <code>DragGestureEvent</code> event
     * 
     */
  
    public void dragGestureRecognized( DragGestureEvent event) {
	InputEvent ipe = event.getTriggerEvent();
	if (ipe.getModifiers() != ipe.BUTTON1_MASK ) {
	    return;
	}
	Object selected = obsList.getSelectedValue();
	obsList.setEnabled(false);
	DeferredProgramList.clearSelection();
	selectedItem = (SpItem)selected;
	if ( selected != null ){
	    SpItem tmp = _spItem.deepCopy();
	    obsToDefer   = selectedItem.deepCopy();
	    SpInsertData spid;
	    if (SpTreeMan.findInstrumentInContext(obsToDefer) == null && instrumentContext !=  null) {
		spid = SpTreeMan.evalInsertInside(instrumentContext, obsToDefer);
		SpTreeMan.insert(spid);
	    }
	    if ( SpTreeMan.findTargetListInContext(obsToDefer) == null  && 
		 targetContext != null &&
		 targetContext.size() != 0) {
		spid = SpTreeMan.evalInsertInside((SpItem)targetContext.firstElement(), obsToDefer);
		SpTreeMan.insert(spid);
	    }
	    _spItem = tmp;
	    StringSelection text = new StringSelection( obsToDefer.toString());
        
	    // as the name suggests, starts the dragging
	    dragSource.startDrag (event, DragSource.DefaultMoveNoDrop, text, this);
	} else {
	    logger.warn( "nothing was selected to drag");   
	}
    }

    /**
     * Implementation of <code>DragSourceListener </code> Interface
     * @param event  <code>DragSourceDragEvent</code> event
     * 
     */
    public void dragEnter (DragSourceDragEvent event) {
    }

    /**
     * Implementation of <code>DragSourceListener </code> Interface
     * @param evt  <code>DragSourceDragEvent</code> event
     * 
     */
    public void dragOver(DragSourceDragEvent evt){
	/* Chnage the cursor to indicate drop allowed */
	evt.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
    }

    /**
     * Implementation of <code>DragSourceListener </code> Interface
     * @param evt  <code>DragSourceEvent</code> event
     * 
     */
    public void dragExit(DragSourceEvent evt){
	evt.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
    }

    /**
     * Implementation of <code>DragSourceListener </code> Interface
     * @param evt  <code>DragSourceDragEvent</code> event
     * 
     */
    public void dropActionChanged(DragSourceDragEvent evt){
    }

    /**
     * Implementation of <code>DragSourceListener </code> Interface
     * @param evt  <code>DragSourceDropEvent</code> event
     * 
     */
    public void dragDropEnd(DragSourceDropEvent evt){
	if (evt.getDropSuccess() == true) {
	    SpObs obs = (SpObs) obsList.getSelectedValue();
	    if (obs != null) {
		if (obs.isOptional() == true) {
		    removeCurrentNode();
		    selectedItem=null;
		}
	    }
	}
	obsList.setEnabled(true);

    }

  public JButton getRunButton () {return run;}
  private Vector haveScaled   = new Vector(); 
  private Vector scaleFactors = new Vector(); 
  private JMenuItem edit;
  private JMenuItem scale;
  private JMenuItem scaleAgain;
  private JPopupMenu popup;

}
