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
 * Copyright (c) 2002-2020 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.plugin.action.mondrian.catalog;

import mondrian.xmla.DataSourcesConfig;
import mondrian.xmla.DataSourcesConfig.Catalog;
import mondrian.xmla.DataSourcesConfig.Catalogs;
import mondrian.xmla.DataSourcesConfig.DataSource;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.pentaho.platform.api.engine.ICacheManager;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.RepositoryFilePermission;
import org.pentaho.platform.api.repository2.unified.data.node.DataNode;
import org.pentaho.platform.api.repository2.unified.data.node.DataProperty;
import org.pentaho.platform.api.repository2.unified.data.node.NodeRepositoryFileData;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.services.importexport.legacy.MondrianCatalogRepositoryHelper;
import org.pentaho.platform.util.XmlTestConstants;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringBufferInputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MondrianCatalogHelperTest {

  private static final String DEFINITION = "mondrian:";

  private MondrianCatalogHelper mch = new MondrianCatalogHelper() {
    protected boolean hasAccess( MondrianCatalog cat, RepositoryFilePermission permission ) {
      return true;
    }
  };

  private Object cacheValue = null;

  ICacheManager cm;

  DataSourcesConfig.DataSources dsList;

  @Mock
  RepositoryFile mockRepositoryFolder;
  @Mock
  RepositoryFile mockRepositoryFile;
  @Mock
  RepositoryFile mockMetatdataFolder;
  @Mock
  DataNode metadataNode;
  @Mock
  NodeRepositoryFileData mockIRepositoryFileData;
  @Mock
  MondrianCatalogRepositoryHelper mockMondrianCatalogRepositoryHelper;

  @Test
  public void testLoadCatalogsIntoCache() {
    setupDsObjects();

    cm = mock( ICacheManager.class );
    when( cm.cacheEnabled( nullable( String.class ) ) ).thenReturn( true );
    when( cm.getFromRegionCache( nullable( String.class ), any() ) ).thenReturn( cacheValue );

    //TODO: probably don't need...

//    doAnswer( invocation -> {
//        cacheValue = invocation.getArguments()[2];
//        return null;
//      }
//    ).when( cm ).putInRegionCache( nullable( String.class ), any(), any() );

    try ( MockedStatic<PentahoSystem> pentahoSystem = mockStatic( PentahoSystem.class ) ) {
      pentahoSystem.when( () -> PentahoSystem.getCacheManager( any() ) ).thenReturn( cm );


      mch.loadCatalogsIntoCache( dsList, null );

      Object cacheValue = cm.getFromRegionCache( null, null );

      Assert.assertTrue( MondrianCatalogCache.class.isInstance( cacheValue ) );
      Map<?, ?> map = ((MondrianCatalogCache) cacheValue).getCatalogs();

      for ( Object item : map.values() ) {
        Assert.assertTrue( MondrianCatalog.class.isInstance( item ) );
        MondrianCatalog catalog = (MondrianCatalog) item;
        assertEquals( DEFINITION, catalog.getDefinition() );
      }
    }
  }

  @Test( timeout = 2000, expected = SAXException.class )
  public void shouldNotFailAndReturnNullWhenMaliciousXmlIsGiven() throws IOException, ParserConfigurationException, SAXException {
    mch.getMondrianXmlDocument( new StringBufferInputStream( XmlTestConstants.MALICIOUS_XML ) );
    fail();
  }

  @Test
  public void shouldNotFailAndReturnNotNullWhenLegalXmlIsGiven() throws Exception {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<slave_config>"
      + "</slave_config>";

    assertNotNull( mch.getMondrianXmlDocument( new StringBufferInputStream( xml ) ) );
  }

  // PPP-4192
  @Test
  public void testInconsistentSynchronizationOfDataSourcesConfig() throws Exception {

    MondrianCatalogHelperTestable mondrianCatalogHelper = new MondrianCatalogHelperTestable();

    AtomicReference<Boolean> failed = new AtomicReference<>();
    failed.set( Boolean.FALSE );

    ExecutorService executorService = Executors.newFixedThreadPool( 2 );

    executorService.execute( () -> {
      System.out.println( "validateSynchronizedDataSourcesConfig()" );
      try {
        mondrianCatalogHelper.validateSynchronizedDataSourcesConfig();
      } catch ( InterruptedException e ) {
        e.printStackTrace();
        failed.set( Boolean.TRUE );
      } catch ( Exception e ) {
        e.printStackTrace();
        failed.set( Boolean.TRUE );
      }
    } );

    executorService.execute( () -> {
      for ( int i = 0; i < 25; i++ ) {
        try {
          Thread.sleep( 10 );
        } catch ( InterruptedException e ) {
          e.printStackTrace();
          failed.set( Boolean.TRUE );
        }
        mondrianCatalogHelper.setDataSourcesConfig( "b" );
      }
      System.out.println( "setDataSourcesConfig() - Finished" );
    } );

    executorService.shutdown();

    if ( executorService.awaitTermination( 5000, TimeUnit.MILLISECONDS ) ) {
      if ( failed.get() ) {
        System.out.println( "Failed multi-thread execution" );
        Assert.fail();
      }
      return;
    }
    System.out.println( "Failed by timeout" );
    Assert.fail();
  }

  @Test
  public void testGetCatalog() {
    cm = mock( ICacheManager.class );
    when( cm.cacheEnabled( nullable( String.class ) ) ).thenReturn( true );
    when( cm.getFromRegionCache( nullable( String.class ), any() ) ).thenReturn( cacheValue );

    //TODO: probably don't need...

//    doAnswer( invocation -> {
//        cacheValue = invocation.getArguments()[2];
//        return null;
//      }
//    ).when( cm ).putInRegionCache( nullable( String.class ), any(), any() );

    try ( MockedStatic<PentahoSystem> pentahoSystem = mockStatic( PentahoSystem.class ) ) {
      pentahoSystem.when( () -> PentahoSystem.getCacheManager( any() ) ).thenReturn( cm );

      IUnifiedRepository unifiedRepository = mock( IUnifiedRepository.class );
      when( unifiedRepository.getFile( eq( "/etc/mondrian" ) ) ).thenReturn( mockRepositoryFolder );
      when( unifiedRepository.getFile( eq( "/etc/mondrian/name/metadata" ) ) ).thenReturn( mockMetatdataFolder );
      //TODO: may need to handle the return null case, not quite sure how to though...
      when( unifiedRepository.getDataForRead( any(), any() ) ).thenReturn( mockIRepositoryFileData );
      when( unifiedRepository.getChildren( any( Serializable.class ) ) ).thenReturn( singletonList( mockRepositoryFile ) );

      pentahoSystem.when( () -> PentahoSystem.get( any(), any() ) ).thenReturn( unifiedRepository );


      when( mockRepositoryFolder.getId() ).thenReturn( 1 );
      when( mockRepositoryFile.getName() ).thenReturn( "name" );
      verify( mockRepositoryFile.getName(), times( 2 ) );
      when( mockMetatdataFolder.getId() ).thenReturn( 2 );
      when( mockIRepositoryFileData.getNode() ).thenReturn( metadataNode );
      when( metadataNode.getProperty( eq( "datasourceInfo" ) ) ).thenReturn( new DataProperty( "datasourceInfo", "datasourceInfo", DataNode.DataPropertyType.STRING ) );
      when( metadataNode.getProperty( eq( "definition" ) ) ).thenReturn( new DataProperty( "definition", "mondrian:/definition", DataNode.DataPropertyType.STRING ) );

      //TODO: seems redundant, can probably remove
      mch.catalogRepositoryHelper = mockMondrianCatalogRepositoryHelper;

      MondrianCatalog cat = mch.getCatalog( "name", null );

      assertNotNull( cat );
      assertEquals( "name", cat.getName() );
      assertEquals( "mondrian:/definition", cat.getDefinition() );
    }
  }

  private void setupDsObjects() {
    dsList = new DataSourcesConfig.DataSources();

    dsList.dataSources = new DataSource[1];
    DataSource ds = new DataSource();
    dsList.dataSources[0] = ds;

    ds.catalogs = new Catalogs();
    ds.catalogs.catalogs = new Catalog[1];

    Catalog ct = new Catalog();
    ct.definition = DEFINITION;

    ds.catalogs.catalogs[0] = ct;
  }


  class MondrianCatalogHelperTestable extends MondrianCatalogHelper {

    public synchronized void validateSynchronizedDataSourcesConfig() throws Exception {
      setDataSourcesConfig( "a" );
      for ( int i = 0; i < 25; i++ ) {
        Thread.sleep( 20 );
        if ( !"a".equals( getDataSourcesConfig() ) ) {
          throw new Exception( "Another thread changed dataSourcesConfig value" );
        }
      }
      System.out.println( "validateSynchronizedDataSourcesConfig() - Finished" );
    }
  }

}
