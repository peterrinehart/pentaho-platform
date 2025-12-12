package org.pentaho.platform.web.http.api.resources;

import org.pentaho.platform.repository2.unified.jcr.JcrRepositoryDumpToFile;
import org.pentaho.platform.repository2.unified.jcr.JcrRepositoryDumpToFile.Mode;
import org.pentaho.platform.repository2.unified.jcr.sejcr.PentahoJcrTemplate;
import org.springframework.extensions.jcr.JcrTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource to trigger JCR repository dump to file.
 */
@Path( "/jcr" )
public class JcrRepositoryDumpResource extends AbstractJaxRSResource {

  @GET
  @Consumes( MediaType.APPLICATION_JSON )
  @Produces( MediaType.APPLICATION_JSON )
  @Path( "/dump" )
  public Response dumpRepository() {
    try {
      JcrTemplate jcrTemplate = PentahoSystem.get( PentahoJcrTemplate.class, "adminJcrTemplate", null );
      TransactionTemplate txnTemplate = PentahoSystem.get( TransactionTemplate.class, "jcrTransactionTemplate", null );
      String repositoryAdminUsername = "pentahoRepoAdmin";
      if ( jcrTemplate == null || txnTemplate == null || repositoryAdminUsername == null ) {
        return Response.status( Response.Status.INTERNAL_SERVER_ERROR )
          .entity( "Missing required system beans" ).build();
      }
      Mode mode = Mode.valueOf( JcrRepositoryDumpToFile.Mode.SYS.name() );
      JcrRepositoryDumpToFile dumper = new JcrRepositoryDumpToFile(
        jcrTemplate, txnTemplate, repositoryAdminUsername, "dumpFileName", mode
      );
      dumper.execute();
      return Response.ok( new DumpResponse( "success", "dumpFileName" ) ).build();
    } catch ( Exception e ) {
      return Response.status( Response.Status.INTERNAL_SERVER_ERROR )
        .entity( new DumpResponse( "error: " + e.getMessage(), null ) ).build();
    }
  }

  public static class DumpRequest {
    public String filename;
    public String mode; // CUSTOM, SYS, DOC
  }

  public static class DumpResponse {
    public String status;
    public String filename;

    public DumpResponse( String status, String filename ) {
      this.status = status;
      this.filename = filename;
    }
  }
}
