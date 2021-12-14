/*!
 *
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 *
 * Copyright (c) 2002-2018 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.plugin.action.kettle;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.pentaho.di.core.logging.KettleLogLayout;
import org.pentaho.di.core.logging.KettleLoggingEvent;
import org.pentaho.di.core.logging.KettleLoggingEventListener;

public class Log4jForwardingKettleLoggingEventListener implements KettleLoggingEventListener {

  public static final String STRING_PENTAHO_DI_LOGGER_NAME = "org.pentaho.di";

  private Logger pentahoLogger;

  private KettleLogLayout layout;

  /**
   * Create a new forwarder from Kettle to Log4j
   * 
   * @param appender
   *          The appender to forward logging to.
   */
  public Log4jForwardingKettleLoggingEventListener( Appender appender ) {
    pentahoLogger = LogManager.getLogger( STRING_PENTAHO_DI_LOGGER_NAME );
    LoggerContext ctx = (LoggerContext) LogManager.getContext( false );
    Configuration config = ctx.getConfiguration();
    LoggerConfig loggerConfig = config.getLoggerConfig( pentahoLogger.getName() );
    loggerConfig.setAdditive( false );


    // ensure all messages get logged in this logger since we filtered it above
    // we do not set the level in the rootLogger so the rootLogger can decide by itself (e.g. in the platform)
    //
    loggerConfig.setLevel( Level.ALL );

    // Now add the appender to the logger so that everything gets routed there...
    //
    loggerConfig.addAppender( appender, Level.ALL, null );

    // The layout
    //
    layout = new KettleLogLayout( true ); // add time
  }

  @Override
  public void eventAdded( KettleLoggingEvent event ) {

    if ( event.getLevel() == org.pentaho.di.core.logging.LogLevel.NOTHING ) {
      return;
    }

    String line = layout.format( event );

    switch ( event.getLevel() ) {
      case ERROR:
        pentahoLogger.log( Level.ERROR, line );
        break;
      case DEBUG:
      case ROWLEVEL:
        pentahoLogger.log( Level.DEBUG, line );
        break;
      default:
        pentahoLogger.log( Level.INFO, line );
        break;
    }
  }
}
