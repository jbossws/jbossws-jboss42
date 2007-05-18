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
package org.jboss.wsintegration.container.jboss42;

//$Id$

import org.jboss.wsintegration.spi.deployment.AbstractDeployer;
import org.jboss.wsintegration.spi.deployment.Deployment;
import org.jboss.wsintegration.spi.deployment.Endpoint;
import org.jboss.wsintegration.spi.deployment.ServiceEndpointPublisher;
import org.jboss.wsintegration.spi.deployment.UnifiedDeploymentInfo;
import org.jboss.wsintegration.spi.deployment.ServiceEndpointPublisher.RewriteResults;

/**
 * A deployer that modifies the web.xml meta data 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class ModifyWebMetaDataDeployer extends AbstractDeployer
{
   private ServiceEndpointPublisher serviceEndpointPublisher;

   public void setServiceEndpointPublisher(ServiceEndpointPublisher serviceEndpointPublisher)
   {
      this.serviceEndpointPublisher = serviceEndpointPublisher;
   }

   public void create(Deployment dep)
   {
      UnifiedDeploymentInfo udi = dep.getContext().getAttachment(UnifiedDeploymentInfo.class);
      if (udi == null)
         throw new IllegalStateException("Cannot obtain unified deployement info");

      RewriteResults results = serviceEndpointPublisher.rewriteWebXml(udi);

      // The endpoint may not have a target bean when 
      // <servlet-class> originally contained a javax.servlet.Servlet
      for (Endpoint ep : dep.getService().getEndpoints())
      {
         if (ep.getTargetBean() == null)
         {
            String servletName = ep.getName().getKeyProperty(Endpoint.SEPID_PROPERTY_ENDPOINT);
            String beanName = results.sepTargetMap.get(servletName);
            if (beanName == null)
               throw new IllegalStateException("Cannot obtain target bean for: " + servletName);

            try
            {
               ClassLoader loader = dep.getClassLoader();
               Class<?> epBean = loader.loadClass(beanName);
               ep.setTargetBean(epBean);
            }
            catch (ClassNotFoundException ex)
            {
               log.warn("Cannot load target bean: " + beanName);
            }
         }
      }
   }
}