package edu.jach.qt.gui;

import java.util.Enumeration;
import java.util.EventListener ;
import java.util.Iterator;
import java.awt.event.KeyListener ;
import javax.swing.JFrame ;
import javax.swing.JScrollPane ;

import javax.swing.JTextPane ;
import java.util.TreeMap ;
import java.io.FileReader ;
import java.io.FileNotFoundException ;
import java.io.IOException ;
import java.util.Vector ;
import javax.swing.JTabbedPane ;
import javax.swing.JPanel ;
import javax.swing.JCheckBox ;
import javax.swing.JLabel ; 
import java.awt.event.ActionListener ;
import java.awt.event.ActionEvent ;

/* Gemini imports */
import gemini.sp.SpItem ;
import gemini.sp.SpType ;
import gemini.sp.SpRootItem ;
import gemini.sp.SpFactory ;

import gemini.sp.SpAvTable ;

/* OT imports */
import jsky.app.ot.OtTreeWidget ;


/**
 * Class to display an Observation as a tree in a new frame.
 */
class TreeViewer implements ActionListener 
{	
	private TreeMap treemap = null ;
	
	/* 
	 * if hide is true, only show named components and values
	 * else show all components and values
	 */
	private static boolean hide = true ;
	
	private JScrollPane scrollTree = null ;
	private JFrame frame = null ;
	private JLabel label = null ;
	private JCheckBox isHidden = null ;
	private JScrollPane scrollValues = null ;
	
	private SpItem currentItem = null ;
	
	/**
	 * Constructor.
	 * Creates a tree view of the input.
	 * @param item  An observation (SpItem class)
	 */	
	public TreeViewer( SpItem item )
	{
		currentItem = item ;
		init() ;
	}
	
	private void init()
	{
		frame = new JFrame() ;
		frame.setSize( 600 , 300 );
		
		JTabbedPane tabs = new JTabbedPane() ;
		
		frame.getContentPane().add( tabs ) ;
		
		scrollTree = new JScrollPane();
		scrollValues = new JScrollPane();
		
		JPanel panel = new JPanel() ;
		
		isHidden = new JCheckBox() ;
		isHidden.setSelected( hide ) ;
		isHidden.addActionListener( this ) ;
		
		label = new JLabel() ;
		label.setText( "Hide values ?" ) ;
		
		panel.add( scrollValues ) ;
		panel.add( isHidden ) ;
		panel.add( label ) ;

		tabs.add( "Tree view" , scrollTree ) ;
		tabs.add( "Name Value pairs" , scrollValues ) ;
		tabs.add( "Preferences" , panel ) ;
		redraw() ;
	}

	public void update( SpItem item )
	{
		currentItem = item ;
		redraw() ;
	}
	
	private void redraw()
	{	
		SpItem item = item() ;
		OtTreeWidget tree = makeTree( item ) ;
		JTextPane values = drawTree( item ) ;	
		
		scrollTree.getViewport().removeAll() ;
		scrollTree.getViewport().add( tree ) ;
		
		scrollValues.getViewport().removeAll() ;
		scrollValues.getViewport().add( values ) ;
		
		frame.setTitle( item.getTitle() );
		frame.setVisible( true );
		frame.requestFocus();
		frame.repaint();
	}
	
	private SpItem item()
	{
		return currentItem ;
	}
	
	private OtTreeWidget makeTree( SpItem item )
	{
		// Construct a new tree
		OtTreeWidget otTree = new OtTreeWidget();
		SpItem[] itemArray = { item };

		// Create a science program to insert this into.
		SpItem root = SpFactory.create( SpType.SCIENCE_PROGRAM );
		otTree.resetProg( ( SpRootItem )root );
		otTree.spItemsAdded( root , itemArray , ( SpItem )null );
		EventListener[] listeners = otTree.getTree().getListeners( KeyListener.class );
		for( int i = 0 ; i < listeners.length ; i++ )
			otTree.getTree().removeKeyListener( ( KeyListener )listeners[ i ] );

		return otTree;
	}
	
	private JTextPane drawTree( SpItem item )
	{
		parseFile() ;
		JTextPane textPane = new JTextPane() ;
		textPane.setEditable(  false  ) ;
		
		StringBuffer buffer = new StringBuffer() ;

		if( treemap != null )
		{
			Vector vector = unrollItem( item , new Vector() ) ;
			
			for( int index = 0 ; index < vector.size() ; index++ )
			{
				String className = item.getClass().getName() ;
				if( !hide || treemap.containsKey( className ) )
				{
					TreeMap values ; 
					if( hide )
						values = ( TreeMap )treemap.get( className ) ;
					else
						values = new TreeMap() ;
					buffer.append( "\n\n" + className + "\n\n" ) ;
					
					SpAvTable table = item.getTable() ;
					
					Iterator keys = table.getAttrIterator() ;
					
					String key ;
					String value ;
					String alias ;
					String separator ;
					
					while( keys.hasNext() )
					{
						key = ( String )keys.next() ;
						
						if( !hide || values.containsKey( key ) )
						{
							alias = ( String )values.get( key ) ;
							if( alias == null )
								alias = key ;
							value = table.get( key ) ;
						
							if( key.length() > 10 )
								separator = "\t" ;
							else
								separator = "\t\t" ;
							buffer.append( "\t" + alias + " :" + separator + value + "\n" ) ;
						}
					}
				}
				item = ( SpItem )vector.elementAt( index ) ;
			}
		}

		textPane.setText( buffer.toString() ) ;
		
		textPane.setCaretPosition( 0 );
		textPane.repaint();
		
		return textPane ;
	}
	
	private Vector unrollItem( SpItem item , Vector vector )
	{
		Enumeration children = item.children() ;
		while( children.hasMoreElements() )
		{
			SpItem nuItem = ( SpItem )children.nextElement() ;
			unrollItem( nuItem , vector ) ;
			vector.add( nuItem ) ;
		}
			
		return vector ;
	}

	private String readFile()
	{
		char[] chars = new char[ 1024 ] ;
		StringBuffer buffer = new StringBuffer() ;
		String returnable = null ;
		
		buffer.append( System.getProperty( "qtConfig" ) ) ;
		int lastSlash = buffer.lastIndexOf( java.io.File.separator ) ;
		buffer.delete( lastSlash + 1 , buffer.length() ) ;
		buffer.append( "qtValues.conf." ) ;
		buffer.append( System.getProperty( "telescope" ).toLowerCase() ) ;
		
		String fileName = buffer.toString() ;
		
		buffer.delete( 0 , buffer.length() ) ;
		
		try
		{
			FileReader file = new FileReader( fileName ) ;
			while( !file.ready() )
				;
			while( file.read( chars ) != -1 )
				buffer.append( chars ) ;
			file.close();
			returnable = buffer.toString() ; 
		}
		catch( FileNotFoundException fnfe ){}
		catch( IOException ioe ){}
		return returnable ;
	}
	
	// Horrible way to parse a file
	private void parseFile()
	{
		if( treemap == null )
		{
			String contents = readFile() ; 
			
			if( contents != null )
			{
				treemap = new TreeMap() ;
				TreeMap values = null ;
				boolean newComponent = true ;
				String component = null ;
				
				String[] lines = contents.split( "\n" ) ;
				for( int index = 0 ; index < lines.length ; index++ )
				{
					String line = lines[ index ].trim() ;
					if( line.matches( "^#.*" ) )
					{
						// comment block
						continue ;
					}
					else if( line.equals( "" ) )
					{
						// new component
						newComponent = true ;
						values = null ;
					}
					else if( line.matches( "[\\w ]*[,]{1}[\\w ]*" ) )
					{
						// value with alias
						String[] items = line.split( "," ) ;
						if( values != null )
							values.put( items[ 0 ].trim() , items[ 1 ].trim() ) ;
					}
					else if( line.matches( "(\\w+\\.{1})+\\w+" ) )
					{
						try
						{
							if( Class.forName( line , false , this.getClass().getClassLoader() ) != null && newComponent )
							{
								component = line ;
								newComponent = false ;
								values = new TreeMap() ;
								treemap.put( component , values ) ;
							}
							else
							{
								if( values != null )
									values.put( line , null ) ;
							}
						}
						catch( ClassNotFoundException cnfe )
						{
							if( values != null )
								values.put( line , null ) ;	
						}
					}
					else if( line.matches( "[\\w]+" ) )
					{
						if( values != null )
							values.put( line , null ) ;
					}
				}
			}
		}
	}

	public void actionPerformed( ActionEvent event )
	{
		Object sauce = event.getSource() ;
		if( sauce instanceof JCheckBox )
		{
			hide = (( JCheckBox )sauce).isSelected() ;
			redraw() ;
		}
	}
	
}