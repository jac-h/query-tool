package edu.jach.qt.gui ;

/* OT imports */
import gemini.util.JACLogger ;

/* QT imports */
import edu.jach.qt.app.Querytool ;
import edu.jach.qt.utils.MsbClient ;
import edu.jach.qt.utils.MsbColumns ;
import edu.jach.qt.utils.SpQueuedMap ;

/* Miscellaneous imports */
/* Standard imports */
import java.awt.GridBagLayout ;
import java.awt.AWTEvent ;
import java.awt.Dimension ;
import java.awt.BorderLayout ;
import java.awt.Component ;
import java.awt.Color ;
import java.awt.event.ActionListener ;
import java.awt.event.ActionEvent ;
import java.awt.event.WindowAdapter ;
import java.awt.event.WindowEvent ;
import java.awt.event.MouseAdapter ;
import java.awt.event.MouseEvent ;
import java.io.IOException ;
import java.io.File ;
import java.net.URL ;
import java.util.Hashtable ;
import java.util.Set ;
import java.util.Vector ;
import javax.swing.JFrame ;
import javax.swing.JTable ;
import javax.swing.JMenuItem ;
import javax.swing.JCheckBoxMenuItem ;
import javax.swing.JTabbedPane ;
import javax.swing.JMenu ;
import javax.swing.JPopupMenu ;
import javax.swing.JOptionPane ;
import javax.swing.JScrollPane ;
import javax.swing.JViewport ;
import javax.swing.JSplitPane ;
import javax.swing.ToolTipManager ;
import javax.swing.ListSelectionModel ;
import javax.swing.SwingUtilities ;
import javax.swing.SwingConstants ;
import javax.swing.ImageIcon ;
import javax.swing.JMenuBar ;
import javax.swing.JButton ;
import javax.swing.event.ChangeEvent ;
import javax.swing.event.MenuListener ;
import javax.swing.event.ListSelectionListener ;
import javax.swing.event.MenuEvent ;
import javax.swing.event.ListSelectionEvent ;
import javax.swing.event.TableColumnModelEvent ;
import javax.swing.event.TableColumnModelListener ;
import javax.swing.table.TableColumnModel ;
import javax.swing.table.TableColumn ;


/**
 * The <code>QtFrame</code> is responsible for how the main JFrame
 * is to look.  It starts 2 panel classes InfoPanel and InputPanel
 * adding them to the left side and top right respectively.  Also,
 * a JTable is created with a sort model that is sensitive to column 
 * header clicks.  Each click sorts rows in descending order relative to 
 * the column clicked.  A shift-click has the effect of an ascending 
 * sort.  The JTable is placed in the bottom right of the JFrame.
 *
 * @author <a href="mailto:mrippa@jach.hawaii.edu">Mathew Rippa</a>
 * $Id$
 */
@SuppressWarnings( "serial" )
public class QtFrame extends JFrame implements ActionListener , MenuListener , ListSelectionListener
{
	private final static String INDEX = "Index" ;
	private final static String EXIT = "Exit" ;
	private final static String LOG = "Log..." ;
	private final static String COLUMNS = "Columns..." ;
	private final static String INFRA_RED = "Infra Red" ;
	private final static String WATER_VAPOUR = "Water Vapour" ;
	private static final String ABOUT = "About" ;

	private static final String WIDGET_CONFIG_FILE = System.getProperty( "widgetFile" ) ;
	static JACLogger logger = JACLogger.getLogger( QtFrame.class ) ;
	private MSBQueryTableModel msbQTM ;
	private JTable projectTable ;
	private QtTable table ;
	private int selRow ;
	private JMenuItem saveItem ;
	private JMenuItem saveAsItem ;
	private JMenuItem exitItem ;
	private JCheckBoxMenuItem disableAll ;
	private JCheckBoxMenuItem observability ;
	private JCheckBoxMenuItem remaining ;
	private JCheckBoxMenuItem allocation ;
	private JCheckBoxMenuItem zoneOfAvoidance ;
	private JTabbedPane tabbedPane ;
	private OmpOM om ;
	private WidgetDataBag widgetBag ;
	private Querytool localQuerytool ;
	private InfoPanel infoPanel ;
	private JPopupMenu popup ;
	private WidgetPanel _widgetPanel ;
	private Hashtable<JTable,int[]> columnSizes = new Hashtable<JTable,int[]>() ;
	private boolean queryExpired = false ;
	private JScrollPane resultsPanel ;
	private JScrollPane projectPane ;
	SwingWorker msbWorker ;
	private boolean menuBuilt = false ;
	private CalibrationsPanel calibrationMenu ;

	public JSplitPane topPanel ;
	public JSplitPane tablePanel ;

	/**
	 * Creates a new <code>QtFrame</code> instance.
	 * 
	 * @param wdb
	 *            a <code>WidgetDataBag</code> value
	 * @param qt
	 *            a <code>Querytool</code> value
	 */
	public QtFrame( WidgetDataBag wdb , Querytool qt )
	{
		widgetBag = wdb ;
		localQuerytool = qt ;

		enableEvents( AWTEvent.WINDOW_EVENT_MASK ) ;
		GridBagLayout layout = new GridBagLayout() ;
		setLayout( layout ) ;
		this.setDefaultCloseOperation( DO_NOTHING_ON_CLOSE ) ;
		this.addWindowListener( new WindowAdapter()
		{
			public void windowClosing( WindowEvent e )
			{
				exitQT() ;
			}
		} ) ;

		try
		{

			om = new OmpOM() ;
			om.addNewTree( null ) ;

			compInit() ;
			tabbedPane.setSelectedIndex( 0 ) ;
			logger.info( "Tree validated" ) ;

                        // Set the top panel's preferred size to its
                        // current size to prevent it expanding if
                        // an excessive amount of text is inserted
                        // into the notes text pane.
                        //
                        // This is intended to fix fault: 20060807.007
                        validate();
                        topPanel.setPreferredSize(topPanel.getSize());
                        logger.info("Top panel size fixed");
		}
		catch( Exception e )
		{
			e.printStackTrace() ;
		}
		logger.info( "Exiting QtFrame constructor" ) ;
	}

	/**
	 * On exit, prompt the user if they want to save any deferred observation, then shutdown.
	 */
	public void exitQT()
	{
		logger.info( "QT shutdown by user" ) ;
		boolean canExit = true ;
		// See if there are any outstanding observations and ask the user what to do with them...
		if( om != null )
			canExit = om.checkProgramTree() ;

		if( !canExit )
		{
			JOptionPane.showMessageDialog( this , "Can not exit the QT until the current MSB is accepted/rejected" , "Can not exit QT" , JOptionPane.WARNING_MESSAGE ) ;
			return ;
		}

		// Run cleanup at shutdown just in case we crossed a UT date boundary
		DeferredProgramList.cleanup() ;
		
		File cacheDir = new File( "/tmp/last_user" ) ;
		if( cacheDir.exists() && cacheDir.isDirectory() )
		{
			File[] files = cacheDir.listFiles() ;
			for( int i = 0 ; i < files.length ; i++ )
			{
				if( files[ i ].isFile() )
					files[ i ].delete() ;
			}
		}
		setVisible( false ) ;
		dispose() ;
		logger.shutdown() ;
		System.exit( 0 ) ;
	}

	/**
	 * Component initialisation. Initialises all of the components on the frame.
	 * 
	 * @exception Exception
	 *                on error.
	 */
	private void compInit() throws Exception
	{
		// Clean up deferred observations from previous UT dates
		DeferredProgramList.cleanup() ;

		// Input Panel Setup
		WidgetPanel inputPanel = new WidgetPanel( new Hashtable<String,String>() , widgetBag ) ;
		_widgetPanel = inputPanel ;
		calibrationMenu = new CalibrationsPanel() ;
		buildStagingPanel() ;
		// Table setup
		try
		{
			msbQTM = new MSBQueryTableModel() ;
		}
		catch( Exception e )
		{
			logger.error( "Unable to create table model" , e ) ;
			e.printStackTrace() ;
			exitQT() ;
		}
		infoPanel = new InfoPanel( msbQTM , localQuerytool , this ) ;

		ProjectTableModel ptm = new ProjectTableModel() ;
		projectTableSetup( ptm ) ;
		tableSetup() ;

		logger.info( "Table setup" ) ;

		resultsPanel = new JScrollPane( table ) ;
		resultsPanel.getViewport().setScrollMode( JViewport.BLIT_SCROLL_MODE ) ;
		projectPane = new JScrollPane( projectTable ) ;
		projectPane.getViewport().setScrollMode( JViewport.BLIT_SCROLL_MODE ) ;

		tablePanel = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT , projectPane , resultsPanel ) ;
		tablePanel.setDividerSize( 0 ) ;
		tablePanel.validate() ;

		tablePanel.setMinimumSize( new Dimension( -1 , 100 ) ) ;

		topPanel = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT , infoPanel , tabbedPane ) ;
		topPanel.setMinimumSize( new Dimension( -1 , 450 ) ) ;
		topPanel.setDividerSize( 0 ) ;
		topPanel.validate() ;

		// Build Menu
		buildMenu() ;

		logger.info( "Menu built" ) ;

		setLayout( new BorderLayout() ) ;
		add( topPanel , BorderLayout.NORTH ) ;
		add( tablePanel , BorderLayout.CENTER ) ;

		// Read the configuration to determine the widgets
		try
		{
			inputPanel.parseConfig( WIDGET_CONFIG_FILE ) ;
		}
		catch( IOException e )
		{
			logger.fatal( "Widget Panel Parse Failed" , e ) ;
		}

		logger.info( "Widget config parsed" ) ;
	}

	private void projectTableSetup( ProjectTableModel ptm )
	{
		Vector<String> columnNames = new Vector<String>() ;
		columnNames.add( "projectid" ) ;
		columnNames.add( "priority" ) ;
		projectTable = new JTable( ptm ) ;
		projectTable.setPreferredScrollableViewportSize( new Dimension( 150 , -1 ) ) ;
		ToolTipManager.sharedInstance().unregisterComponent( projectTable ) ;
		ToolTipManager.sharedInstance().unregisterComponent( projectTable.getTableHeader() ) ;
		projectTable.setAutoResizeMode( JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS ) ;

		projectTable.setSelectionModel( new ProjectTableSelectionModel( this ) ) ;

		projectTable.setVisible( true ) ;

		columnSizes.put( projectTable , new int[ 0 ] ) ;
	}

	private void tableSetup()
	{
		final TableSorter sorter = new TableSorter( msbQTM ) ;
		table = new QtTable( sorter ) ;
		ToolTipManager.sharedInstance().unregisterComponent( table ) ;
		ToolTipManager.sharedInstance().unregisterComponent( table.getTableHeader() ) ;
		sorter.addMouseListenerToHeaderInTable( table ) ;
		table.setAutoResizeMode( JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS ) ;
		table.setMinimumSize( new Dimension( 770 , 275 ) ) ;

		ListSelectionModel listMod = table.getSelectionModel() ;
		listMod.setSelectionMode( ListSelectionModel.SINGLE_SELECTION ) ;
		listMod.addListSelectionListener( this ) ;

		popup = new JPopupMenu( "MSB" ) ;
		JMenuItem menuSendMSB = new JMenuItem( "Send MSB to Staging Area" ) ;
		popup.add( menuSendMSB ) ;
		table.add( popup ) ;
		menuSendMSB.addActionListener( this ) ;

		table.addMouseListener( new MouseAdapter()
		{
			public void mouseClicked( MouseEvent e )
			{
				msbWorker = new SwingWorker()
				{
					Boolean isStatusOK ;
					Integer msbID ;
					MsbColumns columns = MsbClient.getColumnInfo() ;

					public Object construct()
					{
						InfoPanel.logoPanel.start() ;
						logger.info( "Setting up staging panel for the first time." ) ;
						om.enableList( false ) ;

						if( om == null )
							om = new OmpOM() ;

						try
						{
							int checksumIndex = columns.getIndexForKey( "checksum" ) ;
							String checksum = ( String )sorter.getValueAt( selRow , checksumIndex ) ;
							if( remaining.isSelected() )
							{
								String time = SpQueuedMap.getSpQueuedMap().containsMsbChecksum( checksum ) ;
								if( time != null )
								{
									int rtn = JOptionPane.showOptionDialog( null , "This observation was sent to the queue " + time + ".\n Continue ?" , "Duplicate execution warning" , JOptionPane.YES_NO_OPTION , JOptionPane.WARNING_MESSAGE , null , null , null ) ;
									if( rtn == JOptionPane.NO_OPTION )
									{
										isStatusOK = new Boolean( false ) ;
										return isStatusOK ;
									}
								}
							}
							int msbIndex = columns.getIndexForKey( "msbid" ) ;
							msbID = new Integer( ( String )sorter.getValueAt( selRow , msbIndex ) ) ;
							om.setSpItem( localQuerytool.fetchMSB( msbID ) ) ;
							isStatusOK = new Boolean( true ) ;
						}
						catch( Exception e )
						{
							// exceptions are generally Null Pointers or Number Format Exceptions
							JOptionPane.showMessageDialog( null , "Could not fetch MSB" , e.toString() , JOptionPane.ERROR_MESSAGE ) ;
							logger.debug( e.getMessage() ) ;
							isStatusOK = new Boolean( false ) ;
						}
						finally
						{
							om.enableList( true ) ;
							InfoPanel.logoPanel.stop() ;
						}
						return isStatusOK ; // not used yet
					}

					// Runs on the event-dispatching thread.
					public void finished()
					{
						InfoPanel.logoPanel.stop() ;
						om.enableList( true ) ;

						int msbIndex = columns.getIndexForKey( "msbid" ) ;
						msbID = new Integer( ( String )sorter.getValueAt( selRow , msbIndex ) ) ;

						if( isStatusOK )
						{
							DeferredProgramList.clearSelection() ;
							om.addNewTree( msbID ) ;
							buildStagingPanel() ;

							int checksumIndex = columns.getIndexForKey( "checksum" ) ;
							String checksum = ( String )sorter.getValueAt( selRow , checksumIndex ) ;
							int projectIndex = columns.getIndexForKey( "projectid" ) ;
							String projectid = ( String )sorter.getValueAt( selRow , projectIndex ) ;

							logger.info( "MSB " + msbID + " INFO is: " + projectid + ", " + checksum ) ;
						}
						else
						{
							logger.error( "No msb ID retrieved!" ) ;
						}
					}
				} ; // End inner class

				if( SwingUtilities.isLeftMouseButton( e ) && e.getClickCount() == 2 )
				{
					if( selRow == -1 )
						JOptionPane.showMessageDialog( null , "Must select a project summary first!" ) ;
					else
						sendToStagingArea() ;
				}

				else if( SwingUtilities.isRightMouseButton( e ) && e.getClickCount() == 1 )
				{
					logger.debug( "Right Mouse Hit" ) ;
					if( selRow != -1 )
						popup.show( ( Component )e.getSource() , e.getX() , e.getY() ) ;
				}
			}

			public void mousePressed( MouseEvent e ){}

			public void mouseReleased( MouseEvent e ){}

		} ) ;

		TableColumnModel tcm = table.getColumnModel() ;
		TableColumnModelListener mover = new TableColumnModelListener()
		{
			public void columnAdded( TableColumnModelEvent e ){}

			public void columnMarginChanged( ChangeEvent e ){}

			public void columnMoved( TableColumnModelEvent e )
			{
				MsbColumns columns = MsbClient.getColumnInfo() ;
				int from = e.getFromIndex() ;
				int to = e.getToIndex() ;
				columns.move( from , to ) ;
			}

			public void columnRemoved( TableColumnModelEvent e ){}

			public void columnSelectionChanged( ListSelectionEvent e ){}
		} ;
		tcm.addColumnModelListener( mover ) ;

		table.setVisible( true ) ;

		columnSizes.put( table , new int[ 0 ] ) ;
	}

	public void initProjectTable()
	{
		projectTable.getSelectionModel().setSelectionInterval( 0 , 0 ) ;
	}

	public void resetScrollBars()
	{
		if( resultsPanel != null )
			resultsPanel.getVerticalScrollBar().setValue( 0 ) ;
		if( projectPane != null )
			projectPane.getVerticalScrollBar().setValue( 0 ) ;
	}

	public void updateColumnHeaders()
	{
		TableColumnModel tcm = table.getColumnModel() ;
		MsbColumns columns = MsbClient.getColumnInfo() ;
		for( int i = 0 ; i < msbQTM.getColumnCount() ; i++ )
			columns.move( ( String )tcm.getColumn( i ).getHeaderValue() , i ) ;
	}

	/**
	 * Method used to set the current column sizes.  This should be called
	 * before each query.
	 */
	public void updateColumnSizes()
	{
		Set<JTable> tables = columnSizes.keySet() ;

		for( JTable current : tables )
		{
			int[] tableColumnSizes = columnSizes.get( current ) ;
			TableColumnModel tcm = current.getColumnModel() ;
			tableColumnSizes = new int[ current.getColumnCount() ] ;
			columnSizes.put( current , tableColumnSizes ) ;
			for( int i = 0 ; i < current.getModel().getColumnCount() ; i++ )
			{
				int width = tcm.getColumn( i ).getWidth() ;
				tableColumnSizes[ i ] = width ;
			}
		}
	}

	/**
	 * Method to set the column sizes following a query.
	 */
	public void setColumnSizes()
	{
		Set<JTable> tables = columnSizes.keySet() ;
		for( JTable current : tables )
		{
			TableColumnModel tcm = current.getColumnModel() ;
			if( tcm != null )
			{
				int columnCount = tcm.getColumnCount() ;
				int[] tableColumnSizes = columnSizes.get( current ) ;
				for( int i = 0 ; i < tableColumnSizes.length ; i++ )
				{
					if( i >= columnCount )
						break ;
					TableColumn column = tcm.getColumn( i ) ;
					if( column != null )
						column.setPreferredWidth( tableColumnSizes[ i ] ) ;
				}
				current.setColumnModel( tcm ) ;
			}
		}
	}

	/**
	 * Method to redistribute the column widths to the default values.
	 */
	public void setTableToDefault()
	{
		TableColumnModel tcm = table.getColumnModel() ;
		for( int i = 0 ; i < msbQTM.getColumnCount() ; i++ )
			tcm.getColumn( i ).setPreferredWidth( -1 ) ;

		table.setColumnModel( tcm ) ;
	}

	/**
	 * Method to get the current table model.
	 * @return   The current table model.
	 */
	public MSBQueryTableModel getModel()
	{
		return msbQTM ;
	}

	public ProjectTableModel getProjectModel()
	{
		return ( ProjectTableModel )projectTable.getModel() ;
	}

	/**
	 * Sends the selected MSB to the Staging Area.
	 * The MSB must have first been retried and selected from the results table 
	 * on the QT interface.
	 */
	public void sendToStagingArea()
	{
		if( queryExpired )
		{
			String[] options = { "Resubmit" , "New Query" } ;
			int rtn = JOptionPane.showOptionDialog( this , "Query has Expired ;\nNew Query Required" , null , JOptionPane.YES_NO_OPTION , JOptionPane.WARNING_MESSAGE , null , options , options[ 0 ] ) ;
			if( rtn == JOptionPane.YES_OPTION )
			{
				// Resubmit the existing query
				InfoPanel.searchButton.doClick() ;
				return ;
			}
			else
			{
				// Reset the panel to the search panel
				tabbedPane.setSelectedIndex( 0 ) ;
				// Clear the summary model
				msbQTM.clear() ;
				// Clear the project model
				( ( ProjectTableModel )projectTable.getModel() ).clear() ;
				initProjectTable() ;
				return ;
			}
		}

		if( table.getSelectedRow() != -1 )
		{

			msbWorker.start() ;
			om.updateDeferredList() ;
		}
		else
		{
			JOptionPane.showMessageDialog( this , "Must select a project summary first!" ) ;
		}

	}

	/**
	 * Method to get the currently selected tab.
	 * @return  The integer value of the selected tab.
	 */
	public int getSelectedTab()
	{
		return tabbedPane.getSelectedIndex() ;
	}

	/**
	 * Method to set the selected tab.
	 * @param tab   The tab to select.
	 */
	public void setSelectedTab( int tab )
	{
		if( ( tabbedPane.getTabCount() - 1 ) >= tab )
			tabbedPane.setSelectedIndex( tab ) ;
	}

	/**
	 * Build the Staging Area GUI.
	 */
	public void buildStagingPanel()
	{
		if( tabbedPane == null )
		{
			tabbedPane = new JTabbedPane( SwingConstants.TOP ) ;
			tabbedPane.addTab( "Query" , _widgetPanel ) ;
			tabbedPane.addTab( om.getProgramName() , om.getTreePanel() ) ;
			tabbedPane.addTab( "Calibrations" , calibrationMenu ) ;
			tabbedPane.setVisible( true ) ;
		}
		else
		{
			tabbedPane.setTitleAt( 1 , om.getProgramName() ) ;
			om.updateNotes() ;
		}

		tabbedPane.setSelectedIndex( 1 ) ;
	}

	/**
	 *  Method to set the observational parameter to default.
	 */
	public void setMenuDefault()
	{
		if( disableAll.isSelected() )
			disableAll.doClick() ;
		if( !( observability.isSelected() ) )
			observability.doClick() ;
		if( !( remaining.isSelected() ) )
			remaining.doClick() ;
		if( !( allocation.isSelected() ) )
			allocation.doClick() ;
		if( !( zoneOfAvoidance.isSelected() ) )
			zoneOfAvoidance.doClick() ;
	}

	public void setQueryExpired( boolean flag )
	{
		queryExpired = flag ;
	}

	/**
	 * The <code>valueChanged</code> method is mandated by the 
	 * ListSelectionListener. Called whenever the value of 
	 * the selection changes.
	 *
	 * @param e a <code>ListSelectionEvent</code> value
	 */
	public void valueChanged( ListSelectionEvent e )
	{
		if( !e.getValueIsAdjusting() )
			selRow = table.getSelectedRow() ;
	}

	/**
	 * The <code>buildMenu</code> method builds the menu system.
	 * 
	 */
	private void buildMenu()
	{
		JMenuBar mbar = new JMenuBar() ;
		setJMenuBar( mbar ) ;

		JMenu fileMenu = new JMenu( "File" ) ;
		fileMenu.addMenuListener( this ) ;

		JMenuItem openItem = new JMenuItem( "Open" ) ;
		openItem.setEnabled( false ) ;
		JMenuItem newItem = new JMenuItem( "New" ) ;
		newItem.setEnabled( false ) ;
		saveItem = new JMenuItem( "Save" ) ;
		saveItem.setEnabled( false ) ;
		saveAsItem = new JMenuItem( "Save As" ) ;
		saveAsItem.setEnabled( false ) ;
		exitItem = new JMenuItem( EXIT ) ;

		mbar.add( makeMenu( fileMenu , new Object[] { newItem , openItem , null , saveItem , saveAsItem , null , exitItem } , this ) ) ;

		JMenu viewMenu = new JMenu( "View" ) ;
		mbar.add( viewMenu ) ;
		JMenuItem columnItem = new JMenuItem( COLUMNS ) ;
		columnItem.addActionListener( this ) ;
		JMenuItem logItem = new JMenuItem( LOG ) ;
		logItem.addActionListener( this ) ;
		JMenu satMenu = new JMenu( "Satellite Image" ) ;
		JMenuItem irItem = new JMenuItem( INFRA_RED ) ;
		irItem.addActionListener( this ) ;
		JMenuItem wvItem = new JMenuItem( WATER_VAPOUR ) ;
		wvItem.addActionListener( this ) ;
		satMenu.add( irItem ) ;
		satMenu.add( wvItem ) ;
		viewMenu.add( columnItem ) ;
		viewMenu.add( logItem ) ;
		viewMenu.add( satMenu ) ;

		observability = new JCheckBoxMenuItem( "Observability" , true ) ;
		remaining = new JCheckBoxMenuItem( "Remaining" , true ) ;
		allocation = new JCheckBoxMenuItem( "Allocation" , true ) ;

		String ZOA = System.getProperty( "ZOA" , "true" ) ;
		boolean tickZOA = true ;
		if( "false".equalsIgnoreCase( ZOA ) )
			tickZOA = false ;
		zoneOfAvoidance = new JCheckBoxMenuItem( "Zone of Avoidance" , tickZOA ) ;
		localQuerytool.setZoneOfAvoidanceConstraint( !tickZOA ) ;

		disableAll = new JCheckBoxMenuItem( "Disable All" , false ) ;
		JMenuItem cutItem = new JMenuItem( "Cut" , new ImageIcon( "icons/cut.gif" ) ) ;
		cutItem.setEnabled( false ) ;
		JMenuItem copyItem = new JMenuItem( "Copy" , new ImageIcon( "icons/copy.gif" ) ) ;
		copyItem.setEnabled( false ) ;
		JMenuItem pasteItem = new JMenuItem( "Paste" , new ImageIcon( "icons/paste.gif" ) ) ;
		pasteItem.setEnabled( false ) ;

		mbar.add( makeMenu( "Edit" , new Object[] { cutItem , copyItem , pasteItem , null , makeMenu( "Constraints" , new Object[] { observability , remaining , allocation , zoneOfAvoidance , null , disableAll } , this ) } , this ) ) ;

		JMenu helpMenu = new JMenu( "Help" ) ;
		helpMenu.setMnemonic( 'H' ) ;

		mbar.add( makeMenu( helpMenu , new Object[] { new JMenuItem( INDEX , 'I' ) , new JMenuItem( ABOUT , 'A' ) } , this ) ) ;

		menuBuilt = true ;
	}

	/**
	 * Method to run the calibration fetching thread, so it can be done after everything else.
	 */
	public boolean fetchCalibrations()
	{
		calibrationMenu.init() ;
		return menuBuilt ;
	}

	/**
	 * <code>menuSelected</code> method is an action triggered when a menu is selected.
	 * 
	 * @param evt
	 *            a <code>MenuEvent</code> value
	 */
	public void menuSelected( MenuEvent evt )
	{
		JMenu source = ( JMenu )evt.getSource() ;

		if( source.getText().equals( "View..." ) )
			(( JMenuItem )source).setSelected( false ) ;
	}

	/**
	 * <code>menuDeselected</code> method is an action triggered when a 
	 * menu is unselected.
	 *
	 * @param evt a <code>MenuEvent</code> value
	 */
	public void menuDeselected( MenuEvent evt ){}

	/**
	 * The <code>menuCanceled</code> method is an action triggered when a 
	 * menu is cancelled.
	 *
	 * @param evt a <code>MenuEvent</code> value
	 */
	public void menuCanceled( MenuEvent evt ){}

	/**
	 * Implementation of the ActionListener interface. Called on changing a Check Box, Selecting a Menu Item,or pressing a button.
	 * 
	 * @param evt
	 *            an <code>ActionEvent</code> value
	 */
	public void actionPerformed( ActionEvent evt )
	{
		Object source = evt.getSource() ;

		if( source instanceof JCheckBoxMenuItem )
		{
			if( ( JCheckBoxMenuItem )source == disableAll )
			{
				if( disableAll.isSelected() )
				{
					allocation.setSelected( false ) ;
					remaining.setSelected( false ) ;
					observability.setSelected( false ) ;
					zoneOfAvoidance.setSelected( false ) ;
				}
				else
				{
					allocation.setSelected( true ) ;
					remaining.setSelected( true ) ;
					observability.setSelected( true ) ;
					zoneOfAvoidance.setSelected( true ) ;
				}
			}
			else
			{
				disableAll.setSelected( false ) ;
			}
			localQuerytool.setAllocationConstraint( !allocation.isSelected() ) ;
			localQuerytool.setRemainingConstraint( !remaining.isSelected() ) ;
			localQuerytool.setObservabilityConstraint( !observability.isSelected() ) ;
			localQuerytool.setZoneOfAvoidanceConstraint( !zoneOfAvoidance.isSelected() ) ;
			if( allocation.isSelected() && remaining.isSelected() && observability.isSelected() && zoneOfAvoidance.isSelected() )
			{
				// If all selected - set to green light
				URL url = ClassLoader.getSystemResource( "green_light1.gif" ) ;
				ImageIcon icon = new ImageIcon( url ) ;
				InfoPanel.searchButton.setIcon( icon ) ;
				table.setBackground( Color.WHITE ) ;
			}
			else if( !allocation.isSelected() && !remaining.isSelected() && !observability.isSelected() && !zoneOfAvoidance.isSelected() )
			{
				// No constraints disabled - set to red
				URL url = ClassLoader.getSystemResource( "red_light1.gif" ) ;
				ImageIcon icon = new ImageIcon( url ) ;
				InfoPanel.searchButton.setIcon( icon ) ;
				table.setBackground( Color.RED.darker() ) ;
			}
			else
			{
				// Some constraints disabled - set to amber
				URL url = ClassLoader.getSystemResource( "amber_light1.gif" ) ;
				ImageIcon icon = new ImageIcon( url ) ;
				InfoPanel.searchButton.setIcon( icon ) ;
				table.setBackground( Color.YELLOW.darker() ) ;
			}
		}
		else if( source instanceof JMenuItem )
		{
			JMenuItem thisItem = ( JMenuItem )source ;
			String thisText = thisItem.getText() ;

			if( INDEX.equalsIgnoreCase( thisText ) )
			{
				new HelpPage() ;
			}
			else if( ABOUT.equalsIgnoreCase( thisText ) )
			{
				String version = System.getProperty( "version" , "unknown" ) ;
				JOptionPane.showMessageDialog( null , "Build corresponds to Git commit : \n" + version ) ;
			}
			else if( EXIT.equalsIgnoreCase( thisText ) )
			{
				exitQT() ;
			}
			else if( COLUMNS.equalsIgnoreCase( thisText ) )
			{
				new ColumnSelector( this ) ;
			}
			else if( LOG.equalsIgnoreCase( thisText ) )
			{
				LogViewer viewer = new LogViewer() ;
				viewer.showLog( System.getProperty( "QT_LOG_DIR" ) + "/QT.log" ) ;
			}
			else if( INFRA_RED.equalsIgnoreCase( thisText ) )
			{
				infoPanel.getSatPanel().setDisplay( thisItem.getText() ) ;
			}
			else if( WATER_VAPOUR.equalsIgnoreCase( thisText ) )
			{
				infoPanel.getSatPanel().setDisplay( thisItem.getText() ) ;
			}
		}
		else if( source instanceof JButton )
		{
			JButton thisButton = ( JButton )source ;

			if( thisButton.getText().equals( EXIT ) )
			{
				exitQT() ;
			}
			else
			{
				logger.debug( "Popup send MSB" ) ;
				msbWorker.start() ;
			}
		}
	}

	/**
	 * The <code>makeMenu</code> method is a convenience to make a 
	 * generic menu.
	 *
	 * @param parent an <code>Object</code> value
	 * @param items an <code>Object[]</code> value
	 * @param target an <code>Object</code> value
	 * @return a <code>JMenu</code> value
	 */
	public static JMenu makeMenu( Object parent , Object[] items , Object target )
	{
		JMenu m = null ;
		if( parent instanceof JMenu )
			m = ( JMenu )parent ;
		else if( parent instanceof String )
			m = new JMenu( ( String )parent ) ;
		else
			return null ;

		for( int i = 0 ; i < items.length ; i++ )
		{
			if( items[ i ] == null )
				m.addSeparator() ;
			else
				m.add( makeMenuItem( items[ i ] , target ) ) ;
		}

		return m ;
	}

	/**
	 * The <code>makeMenuItem</code> method is a convenience for a 
	 * generic menu item.
	 *
	 * @param item an <code>Object</code> value
	 * @param target an <code>Object</code> value
	 * @return a <code>JMenuItem</code> value
	 */
	public static JMenuItem makeMenuItem( Object item , Object target )
	{
		JMenuItem r = null ;
		if( item instanceof String )
			r = new JMenuItem( ( String )item ) ;
		else if( item instanceof JMenuItem )
			r = ( JMenuItem )item ;
		else
			return null ;

		if( target instanceof ActionListener )
			r.addActionListener( ( ActionListener )target ) ;
		return r ;
	}

	/**
	 * Return the (code>WidgetPanel</code> from this frame.
	 */
	public WidgetPanel getWidgets()
	{
		return _widgetPanel ;
	}

	public void resetCurrentMSB()
	{
		if( om != null )
			om.addNewTree( null ) ;
	}
}//QtFrame
