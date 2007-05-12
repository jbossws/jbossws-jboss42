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
package org.jboss.ws.integration.jboss42.jbossws;

//$Id$

import org.jboss.ws.integration.deployment.AbstractDeployer;
import org.jboss.ws.integration.deployment.Deployment;
import org.jboss.ws.integration.deployment.UnifiedDeploymentInfo;
import org.jboss.ws.metadata.umdm.UnifiedMetaData;

/**
 * A deployer that generates a webapp for an EJB endpoint 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class WebAppGeneratorDeployer extends AbstractDeployer
{
   @Override
   public void create(Deployment dep)
   {
      UnifiedDeploymentInfo udi = dep.getContext().getAttachment(UnifiedDeploymentInfo.class);
      if (udi == null)
         throw new IllegalStateException("Cannot obtain unified deployement info");

      UnifiedMetaData umd = dep.getContext().getAttachment(UnifiedMetaData.class);
      if (umd == null)
         throw new IllegalStateException("Cannot obtain unified meta data");

      if (dep.getType().toString().endsWith("EJB21"))
      {
         ServiceEndpointGeneratorEJB21 generator = new ServiceEndpointGeneratorEJB21();
         udi.webappURL = generator.generatWebDeployment(umd, udi);
      }
      else if (dep.getType().toString().endsWith("EJB3"))
      {
         ServiceEndpointGeneratorEJB3 generator = new ServiceEndpointGeneratorEJB3();
         udi.webappURL = generator.generatWebDeployment(umd, udi);
      }
   }
}