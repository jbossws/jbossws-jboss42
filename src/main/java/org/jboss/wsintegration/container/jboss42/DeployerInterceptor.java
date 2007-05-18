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

import java.util.LinkedList;
import java.util.List;

import org.jboss.deployment.DeploymentInfo;
import org.jboss.deployment.MainDeployerMBean;
import org.jboss.deployment.SubDeployerInterceptorSupport;
import org.jboss.mx.server.Invocation;
import org.jboss.mx.util.MBeanProxy;

/**
 * A deployer service that manages WS4EE compliant Web Services
 *
 * @author Thomas.Diesler@jboss.org
 * @since 03-May-2007
 */
public abstract class DeployerInterceptor extends SubDeployerInterceptorSupport implements DeployerInterceptorMBean
{
   // The main deployer
   private MainDeployerMBean mainDeployer;

   private List<DeployerHook> phaseOneHooks = new LinkedList<DeployerHook>();
   private List<DeployerHook> phaseTwoHooks = new LinkedList<DeployerHook>();

   public void addPhaseOneHook(DeployerHook hook)
   {
      log.debug("Add phase-one deployer hook: " + hook);
      phaseOneHooks.add(hook);
   }

   public void removePhaseOneHook(DeployerHook hook)
   {
      log.debug("Remove phase-one deployer hook: " + hook);
      phaseOneHooks.remove(hook);
   }

   public void addPhaseTwoHook(DeployerHook hook)
   {
      log.debug("Add phase-two deployer hook: " + hook);
      phaseTwoHooks.add(hook);
   }

   public void removePhaseTwoHook(DeployerHook hook)
   {
      log.debug("Remove phase-two deployer hook: " + hook);
      phaseTwoHooks.remove(hook);
   }

   @Override
   protected final Object create(Invocation invocation, DeploymentInfo unit) throws Throwable
   {
      Object retn = invokeNext(invocation);

      for (DeployerHook deployer : phaseOneHooks)
         deployer.deploy(unit);

      return retn;
   }

   @Override
   protected final Object start(Invocation invocation, DeploymentInfo unit) throws Throwable
   {
      Object retn = invokeNext(invocation);

      for (DeployerHook deployer : phaseTwoHooks)
         deployer.deploy(unit);

      return retn;
   }

   @Override
   protected final Object stop(Invocation invocation, DeploymentInfo unit) throws Throwable
   {
      Object retn = invokeNext(invocation);

      for (DeployerHook deployer : phaseTwoHooks)
         deployer.undeploy(unit);

      return retn;
   }

   @Override
   protected final Object destroy(Invocation invocation, DeploymentInfo unit) throws Throwable
   {
      Object retn = invokeNext(invocation);

      for (DeployerHook deployer : phaseOneHooks)
         deployer.undeploy(unit);

      return retn;
   }

   /** Create the deployer service
    */
   protected void createService() throws Exception
   {
      mainDeployer = (MainDeployerMBean)MBeanProxy.get(MainDeployerMBean.class, MainDeployerMBean.OBJECT_NAME, server);
      super.attach();
   }

   /** Destroy the deployer service
    */
   protected void destroyService()
   {
      super.detach();
   }

   /** 
    * Handle all webservice deployment exceptions.
    * You can either simply log the problem and keep the EJB/WAR module
    * alive or undeploy properly.
    */
   protected void handleStartupException(DeploymentInfo di, Throwable th)
   {
      log.error("Cannot startup webservice for: " + di.shortName, th);
      mainDeployer.undeploy(di);
   }

   /** 
    * Handle all webservice deployment exceptions.
    *
    * You can either simply logs the problem and keep the EJB/WAR module
    * alive or undeploy properly.
    */
   protected void handleShutdownException(String moduleName, Throwable th)
   {
      log.error("Cannot shutdown webservice for: " + moduleName, th);
   }
}
