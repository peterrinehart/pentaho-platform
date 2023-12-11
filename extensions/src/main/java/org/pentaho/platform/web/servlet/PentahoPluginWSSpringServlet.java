package org.pentaho.platform.web.servlet;

import com.sun.xml.ws.transport.http.servlet.ServletAdapterList;
import com.sun.xml.ws.transport.http.servlet.SpringBinding;
import com.sun.xml.ws.transport.http.servlet.WSServletDelegate;
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
import java.util.LinkedHashSet;
import java.util.Set;

public class PentahoPluginWSSpringServlet extends HttpServlet {
  protected WSServletDelegate delegate;
  ServletAdapterList adapters = new ServletAdapterList();
  public void init( ServletConfig servletConfig, String pluginPath, ClassLoader pluginClassLoader, GenericApplicationContext beanFactory ) throws ServletException {
    super.init( servletConfig );
    delegate = createPluginWSDelegate( servletConfig, pluginPath, pluginClassLoader, beanFactory );
  }

  @SuppressWarnings( "unchecked" )
  protected WSServletDelegate createPluginWSDelegate( ServletConfig servletConfig, String pluginPath, ClassLoader pluginClassLoader, GenericApplicationContext beanFactory ) {
    ApplicationContext appContext = getAppContext( pluginPath, pluginClassLoader, beanFactory );

    Set<SpringBinding> bindings = new LinkedHashSet<>();

    bindings.addAll( appContext.getBeansOfType( SpringBinding.class ).values() );

    adapters = new ServletAdapterList();
    for ( SpringBinding binding : bindings ) {
      binding.create( adapters );
    }

    return new WSServletDelegate( adapters, getServletContext() );
  }

  public ServletAdapterList getAdapters() {
    return adapters;
  }

  protected ApplicationContext getAppContext( String pluginPath, ClassLoader pluginClassLoader, GenericApplicationContext beanFactoryArg ) {
    ConfigurableWebApplicationContext wac = new XmlWebApplicationContext() {
      @Override
      protected Resource getResourceByPath( String path ) {
        return new FileSystemResource( new File( path ) );
      }

      @Override
      protected void loadBeanDefinitions( DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
        super.loadBeanDefinitions( beanFactoryArg.getDefaultListableBeanFactory() );
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
    delegate.doPost( request, response, getServletContext() );
  }

  protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException {
    delegate.doGet( request, response, getServletContext() );
  }

  protected void doPut( HttpServletRequest request, HttpServletResponse response ) throws ServletException {
    delegate.doPut( request, response, getServletContext() );
  }

  protected void doDelete( HttpServletRequest request, HttpServletResponse response ) throws ServletException {
    delegate.doDelete( request, response, getServletContext() );
  }
}
