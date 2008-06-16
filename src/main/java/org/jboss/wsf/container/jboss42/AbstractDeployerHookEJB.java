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

import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.deployment.DeploymentException;

//$Id$

/**
 * An abstract deployer for EJB Endpoints.
 * Enganges the START lifecylcle of an endpoint.
 *
 * @author Thomas.Diesler@jboss.org
 * @author Heiko.Braun@jboss.com
 * 
 * @since 25-Apr-2007
 */
public abstract class AbstractDeployerHookEJB extends ArchiveDeployerHook
{
   public void deploy(DeploymentInfo unit) throws DeploymentException
   {
      if (!ignoreDeployment(unit) && isWebServiceDeployment(unit))
      {
         super.deploy(unit); // Calls create
         
         log.debug("deploy: " + unit.shortName);
         Deployment dep = getDeployment(unit);
         if (dep == null  || (dep.getState() != Deployment.DeploymentState.CREATED) )
            throw new DeploymentException("Create step failed");

         getRuntime().start(dep);         

         unit.context.put(Deployment.class, dep);
      }
   }
}
