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

// $Id: WebAppDeployerDeployer.java 3183 2007-05-22 13:06:13Z thomas.diesler@jboss.com $

import java.net.URL;

import javax.management.MBeanServer;

import org.jboss.deployment.DeploymentInfo;
import org.jboss.deployment.MainDeployerMBean;
import org.jboss.logging.Logger;
import org.jboss.mx.util.MBeanProxy;
import org.jboss.mx.util.MBeanProxyCreationException;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.wsf.framework.deployment.WebXMLRewriter;
import org.jboss.wsf.spi.deployment.*;

/**
 * Publish the HTTP service endpoint to Tomcat 
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 12-May-2006
 */
public class WebAppDeploymentAspect extends DeploymentAspect
{
   // provide logging
   private static Logger log = Logger.getLogger(WebAppDeploymentAspect.class);

   private WebXMLRewriter webXMLRewriter;

   public void setWebXMLRewriter(WebXMLRewriter serviceEndpointPublisher)
   {
      this.webXMLRewriter = serviceEndpointPublisher;
   }

   public void create(Deployment dep)
   {
      if (dep.getType().toString().endsWith("EJB21") || dep.getType().toString().endsWith("EJB3"))
      {
         UnifiedDeploymentInfo udi = dep.getContext().getAttachment(UnifiedDeploymentInfo.class);
         if (udi == null)
            throw new IllegalStateException("Cannot obtain unified deployement info");

         URL warURL = (URL)dep.getContext().getProperty(WebXMLRewriter.WEBAPP_URL);

         log.debug("publishServiceEndpoint: " + warURL);
         try
         {
            DeploymentInfo di = dep.getContext().getAttachment(DeploymentInfo.class);
            if (di == null)
               throw new IllegalStateException("Cannot obtain DeploymentInfo from context");

            webXMLRewriter.rewriteWebXml(dep);

            // Preserve the repository config
            DeploymentInfo auxdi = new DeploymentInfo(warURL, null, MBeanServerLocator.locateJBoss());
            auxdi.repositoryConfig = di.getTopRepositoryConfig();
            getMainDeployer().deploy(auxdi);
         }
         catch (Exception ex)
         {
            WSFDeploymentException.rethrow(ex);
         }
      }
   }

   public void destroy(Deployment dep)
   {
      UnifiedDeploymentInfo udi = dep.getContext().getAttachment(UnifiedDeploymentInfo.class);
      if (udi == null)
         throw new IllegalStateException("Cannot obtain unified deployement info");

      URL warURL = (URL)dep.getContext().getProperty(WebXMLRewriter.WEBAPP_URL);
      if (warURL == null)
      {
         log.error("Cannot obtain warURL for: " + udi);
         return;
      }

      log.debug("destroyServiceEndpoint: " + warURL);
      try
      {
         getMainDeployer().undeploy(warURL);
      }
      catch (Exception ex)
      {
         WSFDeploymentException.rethrow(ex);
      }
   }

   private MainDeployerMBean getMainDeployer() throws MBeanProxyCreationException
   {
      MBeanServer server = MBeanServerLocator.locateJBoss();
      MainDeployerMBean mainDeployer = (MainDeployerMBean)MBeanProxy.get(MainDeployerMBean.class, MainDeployerMBean.OBJECT_NAME, server);
      return mainDeployer;
   }
}
