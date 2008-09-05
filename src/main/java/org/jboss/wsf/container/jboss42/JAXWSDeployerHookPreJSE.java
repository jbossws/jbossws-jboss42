/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.wsf.container.jboss42;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;
import javax.ejb.Stateless;
import javax.ejb.Stateful;

import org.jboss.deployment.DeploymentInfo;
import org.jboss.metadata.WebMetaData;
import org.jboss.wsf.common.URLLoaderAdapter;
import org.jboss.wsf.spi.deployment.ArchiveDeployment;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.Service;
import org.jboss.wsf.spi.deployment.Deployment.DeploymentType;

/**
 * A deployer JAXWS JSE Endpoints
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class JAXWSDeployerHookPreJSE extends AbstractDeployerHookJSE
{


   public void undeploy(DeploymentInfo unit)
   {
      // let the post deployer hook deal with undeployment
   }

   /** Get the deployemnt type this deployer can handle
    */
   public DeploymentType getDeploymentType()
   {
      return DeploymentType.JAXWS_JSE;
   }

   @Override
   public Deployment createDeployment(DeploymentInfo di)
   {
      ArchiveDeployment dep = newDeployment(di);
      dep.setRootFile(new URLLoaderAdapter(di.localUrl));
      dep.setRuntimeClassLoader(null);
      dep.setType(getDeploymentType());

      Service service = dep.getService();

      WebMetaData webMetaData = (WebMetaData)di.metaData;
      if (webMetaData == null)
         throw new IllegalStateException("Deployment unit does not contain web meta data");

      // Copy the attachments
      dep.addAttachment(WebMetaData.class, webMetaData);

      List<Servlet> servlets = getEndpointBeans(webMetaData, di.annotationsCl);
      for (Servlet servlet : servlets)
      {
         String servletName = servlet.getServletName();
         String servletClass = servlet.getServletClass();

         // Create the endpoint
         Endpoint ep = newEndpoint(servletClass);
         ep.setShortName(servletName);
         service.addEndpoint(ep);
      }

      return dep;
   }

   @Override
   public boolean isWebServiceDeployment(DeploymentInfo unit)
   {
      if (super.isWebServiceDeployment(unit) == false
        || unit.context.get("org.jboss.ws.ejbwebapp")!=null) // Reject EJB im-memory deployments
         return false;

      boolean isWebServiceDeployment = false;
      try
      {
         WebMetaData webMetaData = (WebMetaData)unit.metaData;
         List<Servlet> servlets = getEndpointBeans(webMetaData, unit.annotationsCl);
         isWebServiceDeployment = servlets.size() > 0;
      }
      catch (Exception ex)
      {
         log.error("Cannot process web deployment", ex);
      }

      return isWebServiceDeployment;
   }

   private List<Servlet> getEndpointBeans(WebMetaData webMetaData, ClassLoader loader)
   {
      List<Servlet> servlets = new ArrayList<Servlet>();
      Iterator it = webMetaData.getServletClassMap().entrySet().iterator();
      while (it.hasNext())
      {
         Map.Entry entry = (Entry)it.next();
         String servletName = (String)entry.getKey();
         String servletClassName = (String)entry.getValue();

         // Skip JSPs
         if (servletClassName == null || servletClassName.length() == 0)
            continue;

         try
         {

            // [JBWS-1762] works on re-written web.xml
            // In this case we grap the endpoint classname from the init param
            Map initParams = webMetaData.getServletParams(servletName);
            Iterator iterator = initParams.keySet().iterator();
            while(iterator.hasNext())
            {
               String paramName = (String)iterator.next();
               if(Endpoint.SEPID_DOMAIN_ENDPOINT.equals(paramName))
               {
                  servletClassName = (String)initParams.get(paramName);                  
               }
            }

            Class<?> servletClass = loader.loadClass(servletClassName.trim());
            boolean isWebService = servletClass.isAnnotationPresent(WebService.class);
            boolean isWebServiceProvider = servletClass.isAnnotationPresent(WebServiceProvider.class);

            if (isWebService || isWebServiceProvider)
            {
               // works on standard JSR 109 deployments
               servlets.add(new Servlet(servletName, servletClassName));
            }

         }
         catch (ClassNotFoundException ex)
         {
            log.warn("Cannot load servlet class: " + servletClassName);
            continue;
         }
      }
      return servlets;
   }
}
