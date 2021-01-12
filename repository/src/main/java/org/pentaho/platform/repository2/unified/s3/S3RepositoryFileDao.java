/*!
 *
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 2 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright (c) 2002-2018 Hitachi Vantara. All rights reserved.
 *
 */

package org.pentaho.platform.repository2.unified.s3;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.platform.api.locale.IPentahoLocale;
import org.pentaho.platform.api.repository2.unified.IRepositoryFileData;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.RepositoryFileAcl;
import org.pentaho.platform.api.repository2.unified.RepositoryFileTree;
import org.pentaho.platform.api.repository2.unified.RepositoryRequest;
import org.pentaho.platform.api.repository2.unified.UnifiedRepositoryException;
import org.pentaho.platform.api.repository2.unified.VersionSummary;
import org.pentaho.platform.api.repository2.unified.data.node.NodeRepositoryFileData;
import org.pentaho.platform.api.repository2.unified.data.simple.SimpleRepositoryFileData;
import org.pentaho.platform.repository.RepositoryFilenameUtils;
import org.pentaho.platform.repository2.unified.IRepositoryFileDao;
import org.pentaho.platform.util.RepositoryPathEncoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@SuppressWarnings( "nls" )
public class S3RepositoryFileDao implements IRepositoryFileDao {
  private FileObject rootDir;
  private static List<Character> reservedChars =
    Collections.unmodifiableList( Arrays.asList( new Character[] { '/', '\\', '\t', '\r', '\n' } ) );
  private static List<Character> reservedCharsWindows =
    Collections.unmodifiableList( Arrays.asList( new Character[]{ '?', '*', ':', '<', '>', '|'} ) );
  private boolean IS_WINDOWS;

  public S3RepositoryFileDao() {
    try {
      rootDir = KettleVFS.getFileObject( "s3a://ackbar-development/rinehart_hackathon" );
    } catch ( KettleFileException e ) {
      throw new UnifiedRepositoryException( e );
    }
  }

  public S3RepositoryFileDao( final String baseDir ) {
    try {
      rootDir = KettleVFS.getFileObject( baseDir );
    } catch ( KettleFileException e ) {
      throw new UnifiedRepositoryException( e );
    }
  }

  public S3RepositoryFileDao( File baseDir ) {
    try {
      rootDir = KettleVFS.getFileObject( baseDir.getAbsolutePath() );
    } catch ( KettleFileException e ) {
      throw new UnifiedRepositoryException( e );
    }
  }

  public boolean canUnlockFile( Serializable fileId ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  public File getRootDir() {
    //return new File( rootDir.getAbsolutePath() );
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  private byte[] inputStreamToBytes( InputStream in ) throws IOException {

    ByteArrayOutputStream out = new ByteArrayOutputStream( 4096 );
    byte[] buffer = new byte[ 4096 ];
    int len;

    while ( ( len = in.read( buffer ) ) >= 0 ) {
      out.write( buffer, 0, len );
    }

    in.close();
    out.close();
    return out.toByteArray();
  }

//  public RepositoryFile createFile( Serializable parentFolderId, RepositoryFile file, IRepositoryFileData data,
//      RepositoryFileAcl acl, String versionMessage ) {
//    String fileNameWithPath = RepositoryFilenameUtils.concat( parentFolderId.toString(), file.getName() );
//    FileOutputStream fos = null;
//    File f = new File( fileNameWithPath );
//
//    try {
//      f.createNewFile();
//      fos = new FileOutputStream( f );
//      if ( data instanceof SimpleRepositoryFileData ) {
//        fos.write( inputStreamToBytes( ( (SimpleRepositoryFileData) data ).getInputStream() ) );
//      } else if ( data instanceof NodeRepositoryFileData ) {
//        fos.write( inputStreamToBytes( new ByteArrayInputStream( ( (NodeRepositoryFileData) data ).getNode().toString()
//            .getBytes() ) ) );
//      }
//    } catch ( FileNotFoundException e ) {
//      throw new UnifiedRepositoryException( "Error writing file [" + fileNameWithPath + "]", e );
//    } catch ( IOException e ) {
//      throw new UnifiedRepositoryException( "Error writing file [" + fileNameWithPath + "]", e );
//    } finally {
//      IOUtils.closeQuietly( fos );
//    }
//
//    return internalGetFile( f );
//  }

  public RepositoryFile createFile( Serializable parentFolderId, RepositoryFile file, IRepositoryFileData data,
                                    RepositoryFileAcl acl, String versionMessage ) {
    String fileNameWithPath = RepositoryFilenameUtils.concat( parentFolderId.toString(), file.getName() );
    OutputStream fout = null;
    FileObject f = null;
    try {
      fout = KettleVFS.getOutputStream( fileNameWithPath, false );
      f = KettleVFS.getFileObject( fileNameWithPath );
      if ( data instanceof SimpleRepositoryFileData ) {
        fout.write( inputStreamToBytes( ( (SimpleRepositoryFileData) data ).getInputStream() ) );
      } else if ( data instanceof NodeRepositoryFileData ) {
        fout.write( inputStreamToBytes( new ByteArrayInputStream( ( (NodeRepositoryFileData) data ).getNode().toString()
          .getBytes() ) ) );
      }
    } catch ( IOException | KettleFileException e ) {
      throw new UnifiedRepositoryException( "Error writing file [" + fileNameWithPath + "]", e );
    } finally {
      IOUtils.closeQuietly( fout );
    }

    return internalGetFile( f );
  }

  public RepositoryFile createFolder( Serializable parentFolderId, RepositoryFile file, RepositoryFileAcl acl,
      String versionMessage ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
//    try {
//      String folderNameWithPath = parentFolderId + "/" + file.getName();
//      File newFolder = new File( folderNameWithPath );
//      newFolder.mkdir();
//      final RepositoryFile repositoryFolder = internalGetFile( newFolder );
//      return repositoryFolder;
//    } catch ( Throwable th ) {
//      throw new UnifiedRepositoryException();
//    }
  }

  public void deleteFile( Serializable fileId, String versionMessage ) {
    try {
      File f = new File( fileId.toString() );
      if ( f.isDirectory() ) {
        FileUtils.deleteDirectory( f );
      } else {
        f.delete();
      }
    } catch ( Exception e ) {
      // CHECKSTYLES IGNORE
    }

  }

  public void deleteFileAtVersion( Serializable fileId, Serializable versionId ) {
    deleteFile( fileId, null );
  }

  @Override
  public List<RepositoryFile> getChildren( RepositoryRequest repositoryRequest ) {
    List<RepositoryFile> children = new ArrayList<>();
    try {
      FileObject folder2 = KettleVFS.getFileObject( repositoryRequest.getPath() );
      FileObject[] childArray = folder2.getChildren();
      for ( int i = 0; i < childArray.length; i++ ) {
        children.add( internalGetFile( childArray[i] ) );
      }
    } catch ( FileSystemException | KettleFileException e ) {
      throw new UnifiedRepositoryException( e );
    }
    return children;
  }

  @Deprecated
  public List<RepositoryFile> getChildren( Serializable folderId ) {
    return getChildren( folderId, "", false );
  }

  @Deprecated
  public List<RepositoryFile> getChildren( Serializable folderId, String filter ) {
    return getChildren( folderId, filter, false );
  }

  @Deprecated
  public List<RepositoryFile> getChildren( Serializable folderId, String filter, Boolean showHiddenFiles ) {
    return getChildren( new RepositoryRequest( folderId.toString(), showHiddenFiles, -1, filter ) );
  }

  @SuppressWarnings( "unchecked" )
  public <T extends IRepositoryFileData> T getData( Serializable fileId, Serializable versionId, Class<T> dataClass ) {
    File f = new File( fileId.toString() );
    T data = null;
    try {
      if ( SimpleRepositoryFileData.class.getName().equals( dataClass.getName() ) ) {
        data = (T) new SimpleRepositoryFileData( new FileInputStream( f ), "UTF-8", "text/plain" );
      } else if ( NodeRepositoryFileData.class.getName().equals( dataClass.getName() ) ) {
        throw new UnsupportedOperationException( "This operation is not support by this repository" );
      }

    } catch ( FileNotFoundException e ) {
      throw new UnifiedRepositoryException( e );
    }
    return data;
  }

  public List<RepositoryFile> getDeletedFiles( Serializable folderId, String filter ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  public List<RepositoryFile> getDeletedFiles() {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

//  public RepositoryFile internalGetFile( File f ) {
//
//    RepositoryFile file = null;
//    if ( f.exists() ) {
//      String jcrPath = f.getAbsolutePath().substring( rootDir.getAbsolutePath().length() );
//      if ( jcrPath.length() == 0 ) {
//        jcrPath = "/";
//      } else if ( IS_WINDOWS ) {
//        jcrPath = FilenameUtils.separatorsToUnix( jcrPath );
//      }
//
//      file =
//          new RepositoryFile.Builder( f.getAbsolutePath(), f.getName() ).createdDate( new Date( f.lastModified() ) )
//              .lastModificationDate( new Date( f.lastModified() ) ).folder( f.isDirectory() ).versioned( false ).path(
//                  jcrPath ).versionId( f.getName() ).locked( false ).lockDate( null ).lockMessage( null ).lockOwner(
//                  null ).title( f.getName() ).description( f.getName() ).locale( null ).fileSize( f.length() ).build();
//    }
//    return file;
//
//  }

  public RepositoryFile internalGetFile( FileObject f ) {

    RepositoryFile file = null;

    try {
      file =
        new RepositoryFile.Builder( f.getPublicURIString(), f.getName().getBaseName() )
          .folder( f.getType().equals( FileType.FOLDER ) || f.getType().equals( FileType.IMAGINARY ) ).versioned( false ).path(
          f.getURL().getPath() ).versionId( f.getName().getBaseName() ).locked( false ).lockDate( null ).lockMessage( null )
          .lockOwner( null ).title( f.getName().getBaseName() ).description( f.getName().getBaseName() ).locale( null ).fileSize( 1 )
          .build();
    } catch ( FileSystemException e ) {
      throw new UnifiedRepositoryException( e );
    }

    return file;

  }

  public RepositoryFile getFile( String relPath ) {

    FileObject f = null;
    try {
      f = KettleVFS.getFileObject( getPhysicalFileLocation( relPath ) );
    } catch ( KettleFileException e ) {
      throw new UnifiedRepositoryException( e );
    }
    return internalGetFile( f );
  }

  static String idToPath( String relPath ) {
    relPath = RepositoryPathEncoder.decodeRepositoryPath( relPath );
    return relPath.replaceFirst( "^/?([A-z])/[/\\\\](.*)", "$1:/$2" );
  }

  public RepositoryFile getFile( Serializable fileId, Serializable versionId ) {
    return getFile( fileId.toString() );
  }

  public RepositoryFile getFileByAbsolutePath( String absPath ) {
    return getFile( absPath );
  }

  public RepositoryFile getFileById( Serializable fileId ) {
    return getFile( fileId.toString() );
  }

  public RepositoryFile getFile( String relPath, boolean loadLocaleMaps ) {
    return getFile( relPath );
  }

  public RepositoryFile getFileById( Serializable fileId, boolean loadLocaleMaps ) {
    return getFile( fileId.toString() );
  }

  @Override
  public RepositoryFile getFile( String relPath, IPentahoLocale locale ) {
    return getFile( relPath );
  }

  @Override
  public RepositoryFile getFileById( Serializable fileId, IPentahoLocale locale ) {
    return getFile( fileId.toString() );
  }

  @Override
  public RepositoryFile getFile( String relPath, boolean loadLocaleMaps, IPentahoLocale locale ) {
    return getFile( relPath );
  }

  @Override
  public RepositoryFile getFileById( Serializable fileId, boolean loadLocaleMaps, IPentahoLocale locale ) {
    return getFile( fileId.toString() );
  }


  @Override
  public RepositoryFileTree getTree( RepositoryRequest repositoryRequest ) {

    File root = new File( getPhysicalFileLocation( repositoryRequest.getPath() ) );

    //TODO ACL     
    return getTree( root, repositoryRequest.getDepth().intValue(),
        repositoryRequest.getChildNodeFilter(), repositoryRequest.getTypes() );
  }

  @Deprecated
  public RepositoryFileTree getTree( String relPath, int depth, String filter, boolean showHidden ) {

    File root = new File( getPhysicalFileLocation( relPath ) );

    //TODO ACL     
    return getTree( root, depth, filter, RepositoryRequest.FILES_TYPE_FILTER.FILES_FOLDERS );
  }

  private RepositoryFileTree getTree( final File file, final int depth, final String childNodeFilter,
      RepositoryRequest.FILES_TYPE_FILTER types ) {
//
//    RepositoryFile rootFile = internalGetFile( file );
//
//    List<RepositoryFileTree> children;
//
//    if ( depth != 0 ) {
//      children = new ArrayList<RepositoryFileTree>();
//
//      if ( file.isDirectory() ) {
//
//        File[] childrenArray = file.listFiles();
//
//        for ( File child : childrenArray ) {
//
//          if ( child.isFile() ) {
//
//            if ( types == RepositoryRequest.FILES_TYPE_FILTER.FILES_FOLDERS || types == RepositoryRequest.FILES_TYPE_FILTER.FILES ) {
//              children.add( new RepositoryFileTree( internalGetFile( child ), new ArrayList<>() ) );
//            }
//
//            continue;
//          }
//
//          RepositoryFileTree repositoryChildFileTree = getTree( child, depth - 1, childNodeFilter, types );
//          if ( repositoryChildFileTree != null ) {
//            children.add( repositoryChildFileTree );
//          }
//        }
//      }
//      Collections.sort( children );
//    } else {
//      children = null;
//    }
//    return new RepositoryFileTree( rootFile, children );
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  public List<VersionSummary> getVersionSummaries( Serializable fileId ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  public VersionSummary getVersionSummary( Serializable fileId, Serializable versionId ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
//    RepositoryFile file = getFile( fileId, versionId );
//    List<String> labels = new ArrayList<String>();
//    return new VersionSummary( fileId, ( versionId != null ? versionId : fileId ),
//        false, file.getCreatedDate(), file.getCreatorId(), StringUtils.EMPTY,  labels );
  }

  public void lockFile( Serializable fileId, String message ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  public void moveFile( Serializable fileId, String destRelPath, String versionMessage ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
//    RepositoryFile file = getFileById( fileId );
//    SimpleRepositoryFileData data = getData( fileId, null, SimpleRepositoryFileData.class );
//    deleteFile( fileId, versionMessage );
//    createFile( null, file, data, null, versionMessage );
  }

  public void permanentlyDeleteFile( Serializable fileId, String versionMessage ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  public void restoreFileAtVersion( Serializable fileId, Serializable versionId, String versionMessage ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  public void undeleteFile( Serializable fileId, String versionMessage ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  public void unlockFile( Serializable fileId ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  public RepositoryFile updateFile( RepositoryFile file, IRepositoryFileData data, String versionMessage ) {
    File f = new File( file.getId().toString() );
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream( f, false );
      if ( data instanceof SimpleRepositoryFileData ) {
        fos.write( inputStreamToBytes( ( (SimpleRepositoryFileData) data ).getInputStream() ) );
      } else if ( data instanceof NodeRepositoryFileData ) {
        fos.write( inputStreamToBytes( new ByteArrayInputStream( ( (NodeRepositoryFileData) data ).getNode().toString()
            .getBytes() ) ) );
      }
    } catch ( FileNotFoundException e ) {
      throw new UnifiedRepositoryException( e );
    } catch ( IOException e ) {
      throw new UnifiedRepositoryException( e );
    } finally {
      IOUtils.closeQuietly( fos );
    }

    return getFile( file.getPath() );
  }

  public List<RepositoryFile> getReferrers( Serializable fileId ) {
    throw new UnsupportedOperationException();
  }

  public void setRootDir( File rootDir ) {
    //this.rootDir = rootDir;
  }

  public void setFileMetadata( final Serializable fileId, Map<String, Serializable> metadataMap ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
//    final File targetFile = new File( fileId.toString() );
//    if ( targetFile.exists() ) {
//      FileOutputStream fos = null;
//      try {
//        final File metadataDir = new File( targetFile.getParent() + File.separatorChar + ".metadata" );
//        if ( !metadataDir.exists() ) {
//          metadataDir.mkdir();
//        }
//        final File metadataFile = new File( metadataDir, targetFile.getName() );
//        if ( !metadataFile.exists() ) {
//          metadataFile.createNewFile();
//        }
//
//        final StringBuilder data = new StringBuilder();
//        for ( String key : metadataMap.keySet() ) {
//          data.append( key ).append( '=' );
//          if ( metadataMap.get( key ) != null ) {
//            data.append( metadataMap.get( key ).toString() );
//          }
//          data.append( '\n' );
//        }
//        fos = new FileOutputStream( metadataFile );
//        fos.write( data.toString().getBytes() );
//      } catch ( FileNotFoundException e ) {
//        throw new UnifiedRepositoryException( "Error writing file metadata [" + fileId + "]", e );
//      } catch ( IOException e ) {
//        throw new UnifiedRepositoryException( "Error writing file metadata [" + fileId + "]", e );
//      } finally {
//        IOUtils.closeQuietly( fos );
//      }
//    }
  }

  public Map<String, Serializable> getFileMetadata( final Serializable fileId ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
//    final String metadataFilename =
//        FilenameUtils.concat( FilenameUtils.concat( FilenameUtils.getFullPathNoEndSeparator( fileId.toString() ),
//            ".metadata" ), FilenameUtils.getName( fileId.toString() ) );
//    final Map<String, Serializable> metadata = new HashMap<String, Serializable>();
//    BufferedReader reader = null;
//    try {
//      reader = new BufferedReader( new FileReader( metadataFilename ) );
//      String data = reader.readLine();
//      while ( data != null ) {
//        final int pos = data.indexOf( '=' );
//        if ( pos > 0 ) {
//          final String key = data.substring( 0, pos );
//          final String value = ( data.length() > pos ? data.substring( pos + 1 ) : null );
//          metadata.put( key, value );
//        }
//        data = reader.readLine();
//      }
//    } catch ( FileNotFoundException e ) {
//      // Do nothing ... metadata empty
//    } catch ( IOException e ) {
//      throw new UnifiedRepositoryException( "Error reading metadata [" + fileId + "]", e );
//    } finally {
//      IOUtils.closeQuietly( reader );
//    }
//    return metadata;
  }

  public void copyFile( Serializable fileId, String destAbsPath, String versionMessage ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  public List<RepositoryFile> getDeletedFiles( String origParentFolderPath, String filter ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  @Override
  public List<Character> getReservedChars() {
    List<Character> charList = new ArrayList<Character>();
    String osName = System.getProperty( "os.name" );
    charList.addAll( reservedChars );
    if ( osName.contains( "Windows" ) ) {
      charList.addAll( reservedCharsWindows );
    }
    return charList;
  }

  @Override
  public List<Locale> getAvailableLocalesForFileById( Serializable fileId ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  @Override
  public List<Locale> getAvailableLocalesForFileByPath( String relPath ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  @Override
  public List<Locale> getAvailableLocalesForFile( RepositoryFile repositoryFile ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  @Override
  public Properties getLocalePropertiesForFileById( Serializable fileId, String locale ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  @Override
  public Properties getLocalePropertiesForFileByPath( String relPath, String locale ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  @Override
  public Properties getLocalePropertiesForFile( RepositoryFile repositoryFile, String locale ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  @Override
  public void setLocalePropertiesForFileById( Serializable fileId, String locale, Properties properties ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  @Override
  public void setLocalePropertiesForFileByPath( String relPath, String locale, Properties properties ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  @Override
  public void setLocalePropertiesForFile( RepositoryFile repositoryFile, String locale, Properties properties ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  @Override
  public void deleteLocalePropertiesForFile( RepositoryFile repositoryFile, String locale ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  @Override
  public RepositoryFile updateFolder( RepositoryFile file, String versionMessage ) {
    throw new UnsupportedOperationException( "This operation is not support by this repository" );
  }

  private String getPhysicalFileLocation( String relPath ) {

    if ( StringUtils.isEmpty( relPath ) ) {
      return relPath;
    }

    String physicalFileLocation = relPath;
    if ( relPath.startsWith( "s3a://ackbar-testing/rinehart_hackathon" ) ) {
      physicalFileLocation = relPath;
    } else {
      physicalFileLocation = RepositoryFilenameUtils.concat( "s3a://ackbar-testing/rinehart_hackathon", relPath.substring( RepositoryFilenameUtils.getPrefixLength( relPath ) ) );
    }

    return physicalFileLocation;
  }
}
