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

import org.jboss.ejb3.Ejb3ModuleMBean;
import org.jboss.ejb3.stateless.StatelessContainer;
import org.jboss.logging.Logger;
import org.jboss.mx.util.MBeanProxy;
import org.jboss.mx.util.MBeanProxyCreationException;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.wsf.spi.deployment.SecurityRolesHandler;
import org.jboss.wsf.spi.deployment.UnifiedDeploymentInfo;
import org.jboss.wsf.spi.deployment.WSDeploymentException;
import org.jboss.wsf.spi.utils.DOMUtils;
import org.w3c.dom.Element;

/**
 * Generate a service endpoint deployment for EJB endpoints 
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 12-May-2006
 */
public class SecurityRolesHandlerEJB3 implements SecurityRolesHandler
{
   // logging support
   protected Logger log = Logger.getLogger(SecurityRolesHandlerEJB3.class);

   /** Add the roles from ejb-jar.xml to the security roles
    */
   public void addSecurityRoles(Element webApp, UnifiedDeploymentInfo udi)
   {
      // The container objects below provide access to all of the ejb metadata
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
                  Element securityRole = (Element)webApp.appendChild(DOMUtils.createElement("security-role"));
                  Element roleName = (Element)securityRole.appendChild(DOMUtils.createElement("role-name"));
                  roleName.appendChild(DOMUtils.createTextNode(role));
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
            throw new WSDeploymentException("Cannot obtain EJB3 module: " + objectName);

         return ejb3Module;
      }
      catch (MBeanProxyCreationException ex)
      {
         throw new WSDeploymentException("Cannot obtain proxy to EJB3 module");
      }
   }
}
