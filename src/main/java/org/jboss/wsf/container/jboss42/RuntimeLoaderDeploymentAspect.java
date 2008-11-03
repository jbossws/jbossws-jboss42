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

import org.jboss.metadata.ApplicationMetaData;
import org.jboss.metadata.WebMetaData;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspect;

/**
 * Determines the correct runtime loader for per deployment type
 * and makes it available through the {@link org.jboss.wsf.spi.deployment.Deployment}.
 *
 * @author Heiko.Braun@jboss.com
 */
public class RuntimeLoaderDeploymentAspect extends DeploymentAspect
{

   public void create(Deployment dep)
   {

      // JSE endpoints
      if (dep.getAttachment(WebMetaData.class) != null)
      {
         WebMetaData webMetaData = dep.getAttachment(WebMetaData.class);
         ClassLoader classLoader = webMetaData.getContextLoader();
         dep.setRuntimeClassLoader(classLoader);
      }

      // EJB3 endpoints
      else if (dep.getType() == Deployment.DeploymentType.JAXWS_EJB3)
      {
         // loader provided by the deployer hook
         if(null == dep.getRuntimeClassLoader())
            throw new IllegalArgumentException("Runtime loader not provided");
      }
      
      // EJB21 endpoints
      else if(dep.getAttachment(ApplicationMetaData.class)!=null)
      {
         // loader provided by the deployer hook
         if(null == dep.getRuntimeClassLoader())
            throw new IllegalArgumentException("Runtime loader not provided");
      }

      else
      {
         throw new IllegalArgumentException("Unable to determine runtime loader");
      }
   }
}
