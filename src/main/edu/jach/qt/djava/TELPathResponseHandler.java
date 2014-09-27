/*
 * Copyright (C) 2002-2010 Science and Technology Facilities Council.
 * All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package edu.jach.qt.djava ;

import au.gov.aao.drama.DramaPath ;
import au.gov.aao.drama.DramaTask ;
import au.gov.aao.drama.DramaException ;
import au.gov.aao.drama.DramaMonitor ;
import au.gov.aao.drama.DramaStatus ;
import ocs.utils.CommandReceiver ;
import gemini.util.JACLogger ;

/**
 * <code>TELPathResponseHandler</code> This class is used to
 * handle responses to the GetPath() method.
 *
 * @author <a href="mailto:mrippa@jach.hawaii.edu">Mathew Rippa</a>
 * $Id$ */
public class TELPathResponseHandler extends DramaPath.ResponseHandler
{
	static JACLogger logger = JACLogger.getRootLogger() ;

	private CommandReceiver cr ;

	/**
	 * Constuctor.
	 * @apram p        A DramaPath Object
	 * @param cr       A CommandReceiver object
	 */
	public TELPathResponseHandler( DramaPath p , CommandReceiver cr )
	{
		super( p ) ;
		this.cr = cr ;
		logger.debug( logger.getClass().getName() ) ;
	}

	/** 
	 * Sucess is invoked when we have completed the get path operation.
	 * @param path       A DramaPath Object
	 * @param task       A DramaTask Object
	 * @return           <code>true</code> always.
	 * @exception        DramaException if the monitor task fails.
	 */
	public boolean Success( DramaPath path , DramaTask task ) throws DramaException
	{
		logger.info( "Got path to task " + path.TaskName() + "." ) ;

		// Start the monitor operation.
		DramaMonitor Monitor = new DramaMonitor( path , new QT_MonResponse( cr ) , true , "AIRMASS" ) ;

		// We have sent a new message, so return true.
		return true ;
	}

	/** 
	 * Invoked if the GetPath operation fails.
	 * @param path      A DramaPath Object
	 * @param task      A DramaTask Object
	 * @return          <code>false</code> always
	 * @exception        DramaException if task fails.
	 */
	public boolean Error( DramaPath path , DramaTask task ) throws DramaException
	{
		DramaStatus status = task.GetEntStatus() ;
		logger.warn( "Failed to get path to task \"" + path + "\"" ) ;
		logger.warn( "Failed with status - " + status ) ;

		cr.setPathLock( false ) ;

		return false ;
	}
}

/*
 * $Log$
 * Revision 1.2  2002/07/29 22:40:44  dewitt
 * Updated commenting.
 *
 * Revision 1.1  2002/07/02 08:37:18  mrippa
 * Airmass callback
 *
 * Revision 1.6  2002/04/20 02:52:17  mrippa
 * Updated Log
 *
 * Revision 1.5  2002/04/20 02:41:24  mrippa
 * Added log4j functionality.
 *
 * Revision 1.4  2002/04/01 21:55:37  mrippa
 * Modified the setTaskLock() name to setPathLock()
 *
 * Revision 1.3  2002/03/07 20:27:43  mrippa
 * Unlock the CommandReceiver on error.!
 *
 * Revision 1.2  2002/03/05 22:12:10  mrippa
 * Provides CommanderReceiver to callback to.
 *
 * Revision 1.1  2002/02/24 06:55:06  mrippa
 * Added Drama support for monitoring tau values.
 *
 */
