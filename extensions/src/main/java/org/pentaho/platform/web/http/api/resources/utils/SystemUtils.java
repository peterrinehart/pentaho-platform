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


package org.pentaho.platform.web.http.api.resources.utils;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.api.engine.IUserRoleListService;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.repository2.unified.ServerRepositoryPaths;
import org.pentaho.platform.repository2.unified.jcr.JcrTenantUtils;
import org.pentaho.platform.security.policy.rolebased.actions.AdministerSecurityAction;
import org.pentaho.platform.security.policy.rolebased.actions.PublishAction;
import org.pentaho.platform.security.policy.rolebased.actions.RepositoryCreateAction;
import org.pentaho.platform.security.policy.rolebased.actions.RepositoryReadAction;

import java.util.Collections;
import java.util.List;

public class SystemUtils {

  private static final Log logger = LogFactory.getLog( SystemUtils.class );

  public static boolean canAdminister() {
    IAuthorizationPolicy policy = PentahoSystem.get( IAuthorizationPolicy.class );
    return policy.isAllowed( RepositoryReadAction.NAME ) && policy.isAllowed( RepositoryCreateAction.NAME )
        && ( policy.isAllowed( AdministerSecurityAction.NAME ) );
  }

  public static boolean canUpload( String uploadDir ) {
    IAuthorizationPolicy policy = PentahoSystem.get( IAuthorizationPolicy.class );

    //check if we are admin or have publish permission
    boolean isAdmin = policy.isAllowed( RepositoryReadAction.NAME ) && policy.isAllowed( RepositoryCreateAction.NAME )
      && ( policy.isAllowed( AdministerSecurityAction.NAME ) || policy.isAllowed( PublishAction.NAME ) );

    //the user does not have admin or publish permission, so we will check if the user imports to their home folder
    if ( !isAdmin && !StringUtils.isEmpty( uploadDir ) ) {
      return validateAccessToHomeFolder( uploadDir );
    }

    return isAdmin;
  }

  public static boolean canDownload( String downloadDir ) {
    IAuthorizationPolicy policy = PentahoSystem.get( IAuthorizationPolicy.class );

    IUserRoleListService userRoleListService = PentahoSystem.get( IUserRoleListService.class );
    String tenantedUserName = PentahoSessionHolder.getSession().getName();
    List<String> tenantedUserRoles = userRoleListService.getRolesForUser( JcrTenantUtils.getUserNameUtils().getTenant( tenantedUserName ), tenantedUserName );

    //check if we are admin or have download-roles
    boolean isAdminOrHaveDownloadActionRole = policy.isAllowed( RepositoryReadAction.NAME )
      && policy.isAllowed( RepositoryCreateAction.NAME )
      && ( policy.isAllowed( AdministerSecurityAction.NAME )
      || !Collections.disjoint( tenantedUserRoles, PentahoSystem.getDownloadRolesList() ) );

    //the user does not have admin or download-roles assigned, so we will check if the user downloads from their home folder
    if ( !isAdminOrHaveDownloadActionRole && !StringUtils.isEmpty( downloadDir ) ) {
      return validateAccessToHomeFolder( downloadDir );
    }

    return isAdminOrHaveDownloadActionRole;
  }

  public static boolean validateAccessToHomeFolder( String dir ) {
    IAuthorizationPolicy policy = PentahoSystem.get( IAuthorizationPolicy.class );

    boolean usingHomeFolder = false;
    String tenantedUserName = PentahoSessionHolder.getSession().getName();
    //get user home folder path
    String userHomeFolderPath = ServerRepositoryPaths
      .getUserHomeFolderPath( JcrTenantUtils.getUserNameUtils().getTenant( tenantedUserName ),
        JcrTenantUtils.getUserNameUtils().getPrincipleName( tenantedUserName ) );
    if ( userHomeFolderPath != null && userHomeFolderPath.length() > 0 ) {
      //we pass the relative path so add serverside root folder for every home folder
      usingHomeFolder = ( ServerRepositoryPaths.getTenantRootFolderPath() + dir )
        .contains( userHomeFolderPath );
    }
    if ( !( usingHomeFolder && policy.isAllowed( RepositoryCreateAction.NAME )
      && policy.isAllowed( RepositoryReadAction.NAME ) ) ) {
      return false;
    }

    return true;
  }

}
