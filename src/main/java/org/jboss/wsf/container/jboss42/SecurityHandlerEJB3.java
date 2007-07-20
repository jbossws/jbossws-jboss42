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

import javax.annotation.security.RolesAllowed;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.dom4j.Element;
import org.jboss.annotation.security.SecurityDomain;
import org.jboss.ejb3.Ejb3ModuleMBean;
import org.jboss.ejb3.stateless.StatelessContainer;
import org.jboss.mx.util.MBeanProxy;
import org.jboss.mx.util.MBeanProxyCreationException;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.SecurityHandler;
import org.jboss.wsf.spi.deployment.UnifiedDeploymentInfo;
import org.jboss.wsf.spi.deployment.WSFDeploymentException;

/**
 * Generate a service endpoint deployment for EJB endpoints 
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 12-May-2006
 */
public class SecurityHandlerEJB3 implements SecurityHandler
{
   public void addSecurityDomain(Element jbossWeb, Deployment dep)
   {
      String securityDomain = null;

      UnifiedDeploymentInfo udi = dep.getContext().getAttachment(UnifiedDeploymentInfo.class);
      Ejb3ModuleMBean ejb3Module = getEJB3Module(udi.deployedObject);
      for (Object manager : ejb3Module.getContainers().values())
      {
         if (manager instanceof StatelessContainer)
         {
            StatelessContainer container = (StatelessContainer)manager;

            SecurityDomain anSecurityDomain = (SecurityDomain)container.resolveAnnotation(SecurityDomain.class);
            if (anSecurityDomain != null)
            {
               if (securityDomain != null && !securityDomain.equals(anSecurityDomain.value()))
                  throw new IllegalStateException("Multiple security domains not supported");

               securityDomain = anSecurityDomain.value();
            }
         }
      }

      if (securityDomain != null)
      {
         if (securityDomain.startsWith("java:/jaas/") == false)
            securityDomain = "java:/jaas/" + securityDomain;
         
         jbossWeb.addElement("security-domain").addText(securityDomain);
      }
   }

   public void addSecurityRoles(Element webApp, Deployment dep)
   {
      // The container objects below provide access to all of the ejb metadata
      UnifiedDeploymentInfo udi = dep.getContext().getAttachment(UnifiedDeploymentInfo.class);
      Ejb3ModuleMBean ejb3Module = getEJB3Module(udi.deployedObject);
      for (Object manager : ejb3Module.getContainers().values())
      {
         if (manager instanceof StatelessContainer)
         {
            StatelessContainer container = (StatelessContainer)manager;

            RolesAllowed anRolesAllowed = (RolesAllowed)container.resolveAnnotation(RolesAllowed.class);
            if (anRolesAllowed != null)
            {
               for (String role : anRolesAllowed.value())
               {
                  webApp.addElement("security-role").addElement("role-name").addText(role);
               }
            }
         }
      }
   }

   private Ejb3ModuleMBean getEJB3Module(ObjectName objectName)
   {
      Ejb3ModuleMBean ejb3Module;
      try
      {
         MBeanServer server = MBeanServerLocator.locateJBoss();
         ejb3Module = (Ejb3ModuleMBean)MBeanProxy.get(Ejb3ModuleMBean.class, objectName, server);
         if (ejb3Module == null)
            throw new WSFDeploymentException("Cannot obtain EJB3 module: " + objectName);

         return ejb3Module;
      }
      catch (MBeanProxyCreationException ex)
      {
         throw new WSFDeploymentException("Cannot obtain proxy to EJB3 module");
      }
   }
}
