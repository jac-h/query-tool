package edu.jach.qt.djava;

import au.gov.aao.drama.*;
import ocs.utils.CommandReceiver;
import org.apache.log4j.Logger;

/**
 * <code>CSOPathResponseHandler</code> This class is used to
 * handle responses to the GetPath() method.
 *
 * @author <a href="mailto:mrippa@jach.hawaii.edu">Mathew Rippa</a>
 * $Id$ */
public class CSOPathResponseHandler extends DramaPath.ResponseHandler {

  static Logger logger = Logger.getRootLogger();

  private CommandReceiver cr;
  public CSOPathResponseHandler(DramaPath p, CommandReceiver cr) {
    super(p);
    this.cr = cr;
    logger.debug(logger.getClass().getName());
  }

  /** 
   * Sucess is invoked when we have completed the get path operation.
   */
  public boolean Success(DramaPath path, DramaTask task) throws DramaException {
    
    // Informational message
    //task.MsgOut("Got path to task "+path.TaskName() +".");
    logger.info("Got path to task "+path.TaskName() +".");
    
    // Start the monitor operation.
    DramaMonitor Monitor = new DramaMonitor(path, new CSO_MonResponse(cr), true, "CSOTAU");

    // We have sent a new message, so return true.
    return true;
  }

  /** 
   * Invoked if the GetPath operation fails
   */
  public boolean Error(DramaPath path, DramaTask task)  throws DramaException {
    DramaStatus status = task.GetEntStatus();
    logger.warn("Failed to get path to task \"" + path + "\"");
    logger.warn("Failed with status - " + status);

    cr.setPathLock(false);

    return false;
  }

}

/*
 * $Log$
 * Revision 1.5  2002/04/20 02:41:24  mrippa
 * Added log4j functionality.
 *
 */
