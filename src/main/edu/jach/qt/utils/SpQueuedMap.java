package edu.jach.qt.utils ;

import gemini.sp.SpItem;
import gemini.sp.SpMSB;
import gemini.sp.SpObs ;

public class SpQueuedMap extends QueuedMap
{

	private static SpQueuedMap queuedMap = null ;
	
	private SpQueuedMap() 
	{
		super() ;
	}

	public static synchronized SpQueuedMap getSpQueuedMap()
	{
		if( queuedMap == null )
			queuedMap = new SpQueuedMap() ;
		return queuedMap ;
	}
	
	public boolean putSpItem( SpItem item )
	{
		if( item == null )
			return false ;
		String checksum = msbChecksum( item ) ;
		if( !checksum.equals( "" ) )
		{
			boolean replacement = treeMap.containsKey( checksum ) ;
			treeMap.put( checksum , null ) ;	
			return replacement ;
		}
		return put( item.toXML() , null ) ;
	}
	
	public boolean containsSpItem( SpItem item )
	{
		if( item == null )
			return false ;
		item = getCorrectItem( item ) ;
		String checksum = msbChecksum( item ) ;
		if( checksum.equals( "" ) )
			checksum = hash( item.toXML() ) ;
		return treeMap.containsKey( checksum ) ;
	}
	
	private String msbChecksum( SpItem item )
	{
		if( item instanceof SpMSB ) 
		{
			SpMSB msb = ( SpMSB )item ;
			return msb.getChecksum() ;
		}		
		return "" ;
	}

	private SpItem getCorrectItem( SpItem item )
	{
		SpItem spitem = ( SpItem )item ;
		if( item instanceof SpObs )
		{
			SpItem parent = spitem.parent() ;
			if( parent instanceof SpMSB )
				spitem = parent ;
		}		
		return spitem ;
	}
	
}
