package edu.jach.qt.gui ;

import java.awt.Component;
import java.awt.GridLayout ;
import java.awt.event.ActionListener ;
import java.awt.event.ActionEvent ;
import java.util.ListIterator ;
import java.util.LinkedList ;
import javax.swing.JRadioButton ;
import javax.swing.ButtonGroup ;
import javax.swing.BorderFactory ;
import javax.swing.border.TitledBorder ;
import java.util.Hashtable ;
import edu.jach.qt.gui.WidgetDataBag ;

/**
 * RadioPanel.java
 *
 * This class is a generic radioPanel with it's group of JRadioButtons
 * enclosed with a titled border.
 *
 * Created: Tue Aug  7 09:35:40 2001
 *
 * @author <a href="mailto:mrippa@jach.hawaii.edu">Mathew Rippa</a>
 * $Id$
 */
@SuppressWarnings( "serial" )
public class RadioPanel extends WidgetPanel implements ActionListener
{
	private JRadioButton rb ;
	private String next , myTitle ;
	private LinkedList<String> myElems ;
	private ListIterator<String> iterator ;

	/**
	 * The variable <code>group</code> is the list of JRadioButtons.
	 *
	 */
	public ButtonGroup group = new ButtonGroup() ;

	/**
	 * The variable <code>radioElems</code> is a LinkedList of the buttons.
	 *
	 */
	public LinkedList<JRadioButton> radioElems = new LinkedList<JRadioButton>() ;

	/**
	 * Creates a new <code>RadioPanel</code> instance.
	 *
	 * @param ht a <code>Hashtable</code> value
	 * @param wdb a <code>WidgetDataBag</code> value
	 * @param title a <code>String</code> value
	 * @param elems a <code>LinkedList</code> value
	 */
	public RadioPanel( Hashtable<String,String> ht , WidgetDataBag wdb , CompInfo info )
	{
		super( ht , wdb ) ;
		myTitle = info.getTitle() ;
		myElems = info.getList() ;
		config() ;
	}

	private void config()
	{
		iterator = myElems.listIterator( 0 ) ;
		setOpaque( false ) ;
		setBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder() , myTitle , TitledBorder.CENTER , TitledBorder.DEFAULT_POSITION ) ) ;
		if( myElems.size() > 3 )
			setLayout( new GridLayout( 0 , 2 ) ) ;
		else
			setLayout( new GridLayout( 3 , 0 ) ) ;

		for( iterator.nextIndex() ; iterator.hasNext() ; iterator.nextIndex() )
		{
			next = iterator.next() ;
			String toolTip = null ;
			if( next.matches( ".*-.*" ) )
			{
				String[] split = next.split( "-" ) ;
				next = split[ 0 ].trim() ;
				toolTip = split[ 1 ].trim() ;
			}
			rb = new JRadioButton( next ) ;
			rb.addActionListener( this ) ;
			if( toolTip != null )
				rb.setToolTipText( toolTip ) ;

			add( rb ) ;
			rb.setAlignmentX( Component.LEFT_ALIGNMENT ) ;
			group.add( rb ) ;
			radioElems.add( rb ) ;
			if( ( iterator.nextIndex() - 1 ) == 0 )
				rb.doClick() ;
		}
	}

	/**
	 * The <code>actionPerformed</code> method will notify the
	 * WidgetDataBag of changes.  This updates the XML string contained
	 * in the Querytool.
	 *
	 * @param evt an <code>ActionEvent</code> value
	 */
	public void actionPerformed( ActionEvent evt )
	{
		setAttribute( myTitle , radioElems ) ;
	}
}// RadioPanel
