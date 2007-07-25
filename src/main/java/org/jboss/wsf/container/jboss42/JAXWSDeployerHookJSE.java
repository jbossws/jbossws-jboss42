/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

//$Id$

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;

import org.jboss.deployment.DeploymentInfo;
import org.jboss.metadata.WebMetaData;
import org.jboss.ws.integration.URLLoaderAdapter;
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
public class JAXWSDeployerHookJSE extends AbstractDeployerHookJSE
{
   /** Get the deployemnt type this deployer can handle 
    */
   public DeploymentType getDeploymentType()
   {
      return DeploymentType.JAXWS_JSE;
   }

   @Override
   public Deployment createDeployment(DeploymentInfo unit)
   {
      Deployment dep = createDeployment();
      dep.setRootFile(new URLLoaderAdapter(unit.localUrl));
      dep.setInitialClassLoader(unit.annotationsCl);
      dep.setRuntimeClassLoader(null);
      dep.setDeploymentType(getDeploymentType());

      Service service = dep.getService();

      WebMetaData webMetaData = (WebMetaData)unit.metaData;
      if (webMetaData == null)
         throw new IllegalStateException("Deployment unit does not contain web meta data");

      // Copy the attachments
      dep.getContext().addAttachment(WebMetaData.class, webMetaData);

      List<Servlet> servlets = getRelevantServlets(webMetaData, unit.annotationsCl);
      for (Servlet servlet : servlets)
      {
         String servletName = servlet.getServletName();
         String servletClass = servlet.getServletClass();

         // Create the endpoint
         Endpoint ep = createEndpoint();
         ep.setShortName(servletName);
         ep.setService(service);
         ep.setTargetBeanName(servletClass);

         service.addEndpoint(ep);
      }

      return dep;
   }

   @Override
   public boolean isWebServiceDeployment(DeploymentInfo unit)
   {
      if (super.isWebServiceDeployment(unit) == false)
         return false;

      boolean isWebServiceDeployment = false;
      try
      {
         WebMetaData webMetaData = (WebMetaData)unit.metaData;
         List<Servlet> servlets = getRelevantServlets(webMetaData, unit.annotationsCl);
         isWebServiceDeployment = servlets.size() > 0;
      }
      catch (Exception ex)
      {
         log.error("Cannot process web deployment", ex);
      }

      return isWebServiceDeployment;
   }

   private List<Servlet> getRelevantServlets(WebMetaData webMetaData, ClassLoader loader)
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
            Class<?> servletClass = loader.loadClass(servletClassName.trim());
            boolean isWebService = servletClass.isAnnotationPresent(WebService.class);
            boolean isWebServiceProvider = servletClass.isAnnotationPresent(WebServiceProvider.class);
            if (isWebService || isWebServiceProvider)
               servlets.add(new Servlet(servletName, servletClassName));
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