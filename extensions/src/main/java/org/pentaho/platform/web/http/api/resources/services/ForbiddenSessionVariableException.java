/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/

package org.pentaho.platform.web.http.api.resources.services;

/**
 * Exception thrown when a session variable operation is not allowed due to
 * whitelist restrictions.
 */
public class ForbiddenSessionVariableException extends RuntimeException {
  public ForbiddenSessionVariableException( String message ) {
    super( message );
  }
}
