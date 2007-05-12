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

import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.deployment.DeploymentInfo;
import org.jboss.logging.Logger;
import org.jboss.mx.util.MBeanProxy;
import org.jboss.mx.util.MBeanProxyCreationException;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.ws.integration.deployment.DeployerManager;
import org.jboss.ws.integration.deployment.WSDeploymentException;
import org.jboss.ws.integration.jboss42.DeployerHook;
import org.jboss.ws.integration.jboss42.DeployerInterceptorMBean;

/**
 * An abstract web service deployer.
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public abstract class AbstractDeployerHook implements DeployerHook
{
   // provide logging
   protected final Logger log = Logger.getLogger(getClass());

   protected DeployerManager deployerManager;
   private List<ObjectName> phaseOneInterceptors;
   private List<ObjectName> phaseTwoInterceptors;

   public void setDeployerManager(DeployerManager deploymentManager)
   {
      this.deployerManager = deploymentManager;
   }

   public void setPhaseOneInterceptors(List<ObjectName> phaseOneInterceptors)
   {
      this.phaseOneInterceptors = phaseOneInterceptors;
   }

   public void setPhaseTwoInterceptors(List<ObjectName> phaseTwoInterceptors)
   {
      this.phaseTwoInterceptors = phaseTwoInterceptors;
   }

   /** Return true if this deployment should be ignored
    */
   public boolean ignoreDeployment(DeploymentInfo unit)
   {
      return false;
   }

   /** Add the hooks to the interceptors
    */
   public void start()
   {
      MBeanServer server = MBeanServerLocator.locateJBoss();
      try
      {
         if (phaseOneInterceptors != null)
         {
            for (ObjectName oname : phaseOneInterceptors)
            {
               DeployerInterceptorMBean interceptor = (DeployerInterceptorMBean)MBeanProxy.get(DeployerInterceptorMBean.class, oname, server);
               interceptor.addPhaseOneHook(this);
            }
         }
         
         if (phaseTwoInterceptors != null)
         {
            for (ObjectName oname : phaseTwoInterceptors)
            {
               DeployerInterceptorMBean interceptor = (DeployerInterceptorMBean)MBeanProxy.get(DeployerInterceptorMBean.class, oname, server);
               interceptor.addPhaseTwoHook(this);
            }
         }
      }
      catch (MBeanProxyCreationException e)
      {
         throw new WSDeploymentException(e);
      }
   }
   
   /** Add the hooks to the interceptors
    */
   public void stop()
   {
      MBeanServer server = MBeanServerLocator.locateJBoss();
      try
      {
         if (phaseOneInterceptors != null)
         {
            for (ObjectName oname : phaseOneInterceptors)
            {
               DeployerInterceptorMBean interceptor = (DeployerInterceptorMBean)MBeanProxy.get(DeployerInterceptorMBean.class, oname, server);
               interceptor.removePhaseOneHook(this);
            }
         }
         
         if (phaseTwoInterceptors != null)
         {
            for (ObjectName oname : phaseTwoInterceptors)
            {
               DeployerInterceptorMBean interceptor = (DeployerInterceptorMBean)MBeanProxy.get(DeployerInterceptorMBean.class, oname, server);
               interceptor.removePhaseTwoHook(this);
            }
         }
      }
      catch (MBeanProxyCreationException e)
      {
         throw new WSDeploymentException(e);
      }
   }
}
