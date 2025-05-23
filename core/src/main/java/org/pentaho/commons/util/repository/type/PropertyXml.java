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


package org.pentaho.commons.util.repository.type;

public class PropertyXml extends PropertyString {

  public PropertyXml( String name ) {
    super( name, new PropertyType( PropertyType.XML ) );
  }

  public PropertyXml( String name, String value ) {
    super( name, new PropertyType( PropertyType.XML ) );
    setValue( value );
  }

}
