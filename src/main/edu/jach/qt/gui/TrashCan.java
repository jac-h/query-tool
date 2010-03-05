package edu.jach.qt.gui ;

import javax.swing.ImageIcon ;
import javax.swing.JLabel ;
import java.net.URL ;
import java.net.MalformedURLException ;

/**
 * Ultra simplistic Trashcan class.
 * All it does is associate a icon with a label!
 */
@SuppressWarnings( "serial" )
public class TrashCan extends JLabel
{
	public static final String BIN_IMAGE = System.getProperty( "binImage" ) ;
	public static final String BIN_SEL_IMAGE = System.getProperty( "binImage" ) ;

	/**
	 * Contructor.
	 */
	public TrashCan()
	{
		try
		{
			URL url = new URL( "file://" + BIN_IMAGE ) ;
			setIcon( new ImageIcon( url ) ) ;
		}
		catch( MalformedURLException mue )
		{
			setIcon( new ImageIcon( ProgramTree.class.getResource( "file://" + BIN_IMAGE ) ) ) ;
		}
	}
}
