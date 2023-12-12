package org.pentaho.platform.web.servlet;

import com.sun.xml.ws.transport.http.servlet.ServletAdapterList;
import com.sun.xml.ws.transport.http.servlet.SpringBinding;
import com.sun.xml.ws.transport.http.servlet.WSServletDelegate;
import org.pentaho.platform.api.engine.IPlatformPlugin;
import org.pentaho.platform.api.engine.IPluginProvider;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.services.pluginmgr.PentahoSystemPluginManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class PentahoPluginWSSpringServlet extends HttpServlet {
  //protected WSServletDelegate delegate;
  private ServletConfig servletConfig;
  Map<String, WSServletDelegate> delegateMap = new HashMap<>();
  ServletAdapterList adapters = new ServletAdapterList();
  public void init( ServletConfig servletConfig ) throws ServletException {
    super.init( servletConfig );
    this.servletConfig = servletConfig;
    //delegate = createPluginWSDelegate( servletConfig, pluginPath, pluginClassLoader, beanFactory );
  }

  @SuppressWarnings( "unchecked" )
  protected WSServletDelegate createPluginWSDelegate( ServletConfig servletConfig, String pluginPath, ClassLoader pluginClassLoader, GenericApplicationContext beanFactory ) {
    ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader( pluginClassLoader );
      ApplicationContext appContext = getAppContext( pluginPath, pluginClassLoader, beanFactory );

      Set<SpringBinding> bindings = new LinkedHashSet<>();

      bindings.addAll( appContext.getBeansOfType( SpringBinding.class ).values() );

      adapters = new ServletAdapterList();
      for ( SpringBinding binding : bindings ) {
        binding.create( adapters );
      }

      return new WSServletDelegate( adapters, getServletContext() );
    } finally {
      Thread.currentThread().setContextClassLoader( origClassLoader );
    }
  }

  public ServletAdapterList getAdapters() {
    return adapters;
  }

  protected ApplicationContext getAppContext( String pluginPath, ClassLoader pluginClassLoader, GenericApplicationContext beanFactoryArg ) {
    XmlWebApplicationContext wac = new XmlWebApplicationContext() {
      @Override
      protected Resource getResourceByPath( String path ) {
        return new FileSystemResource( new File( path ) );
      }

      @Override
      protected DefaultListableBeanFactory createBeanFactory() {
        DefaultListableBeanFactory beanFactory = super.createBeanFactory();
        beanFactory.setBeanClassLoader( pluginClassLoader );
        return beanFactory;
      }
    };

    wac.setServletContext( getServletContext() );
    wac.setServletConfig( getServletConfig() );
    wac.setNamespace( getServletName() );
    wac.setClassLoader( pluginClassLoader );

    String springFile = pluginPath + File.separator + "plugin.spring.xml";  //$NON-NLS-1$ //$NON-NLS-2$
    wac.setConfigLocations( new String[] { springFile } );
    wac.refresh();

    return wac;
  }

  protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException {
    WSServletDelegate delegate = delegateMap.get( request.getRequestURI() );
    if ( null == delegate ) {
      IPlatformPlugin plugin = PentahoSystem.get( IPlatformPlugin.class, null, Collections.singletonMap( PentahoSystemPluginManager.PLUGIN_ID, "scheduler-plugin" )  );
      GenericApplicationContext beanFactory = PentahoSystem
        .get( GenericApplicationContext.class, null, Collections.singletonMap( PentahoSystemPluginManager.PLUGIN_ID, plugin.getId() ) );
      delegate = createPluginWSDelegate( servletConfig
        , PentahoSystem.getApplicationContext().getSolutionPath( "system/" + plugin.getSourceDescription() )
        , beanFactory.getClassLoader()
        , beanFactory );
      delegateMap.put( request.getRequestURI(), delegate );
    }
    delegate.doPost( request, response, getServletContext() );
}

  protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException {
    WSServletDelegate delegate = delegateMap.get( request.getRequestURI() );
    if ( null == delegate ) {
      IPlatformPlugin plugin = PentahoSystem.get( IPlatformPlugin.class, null, Collections.singletonMap( PentahoSystemPluginManager.PLUGIN_ID, "scheduler-plugin" )  );
      GenericApplicationContext beanFactory = PentahoSystem
        .get( GenericApplicationContext.class, null, Collections.singletonMap( PentahoSystemPluginManager.PLUGIN_ID, plugin.getId() ) );
      delegate = createPluginWSDelegate( servletConfig
        , PentahoSystem.getApplicationContext().getSolutionPath( "system/" + plugin.getSourceDescription() )
        , beanFactory.getClassLoader()
        , beanFactory );
      delegateMap.put( request.getRequestURI(), delegate );
    }
    delegate.doGet( request, response, getServletContext() );
  }

  protected void doPut( HttpServletRequest request, HttpServletResponse response ) throws ServletException {
    WSServletDelegate delegate = delegateMap.get( request.getRequestURI() );
    if ( null == delegate ) {
      IPlatformPlugin plugin = PentahoSystem.get( IPlatformPlugin.class, null, Collections.singletonMap( PentahoSystemPluginManager.PLUGIN_ID, "scheduler-plugin" )  );
      GenericApplicationContext beanFactory = PentahoSystem
        .get( GenericApplicationContext.class, null, Collections.singletonMap( PentahoSystemPluginManager.PLUGIN_ID, plugin.getId() ) );
      delegate = createPluginWSDelegate( servletConfig
        , PentahoSystem.getApplicationContext().getSolutionPath( "system/" + plugin.getSourceDescription() )
        , beanFactory.getClassLoader()
        , beanFactory );
      delegateMap.put( request.getRequestURI(), delegate );
    }
    delegate.doPut( request, response, getServletContext() );
  }

  protected void doDelete( HttpServletRequest request, HttpServletResponse response ) throws ServletException {
    WSServletDelegate delegate = delegateMap.get( request.getRequestURI() );
    if ( null == delegate ) {
      IPlatformPlugin plugin = PentahoSystem.get( IPlatformPlugin.class, null, Collections.singletonMap( PentahoSystemPluginManager.PLUGIN_ID, "scheduler-plugin" )  );
      GenericApplicationContext beanFactory = PentahoSystem
        .get( GenericApplicationContext.class, null, Collections.singletonMap(PentahoSystemPluginManager.PLUGIN_ID, plugin.getId() ) );
      delegate = createPluginWSDelegate( servletConfig
        , PentahoSystem.getApplicationContext().getSolutionPath( "system/" + plugin.getSourceDescription() )
        , beanFactory.getClassLoader()
        , beanFactory );
      delegateMap.put( request.getRequestURI(), delegate );
    }
    delegate.doDelete( request, response, getServletContext() );
  }
}
