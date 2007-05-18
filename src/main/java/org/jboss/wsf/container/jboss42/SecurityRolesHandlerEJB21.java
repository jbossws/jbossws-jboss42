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

import java.util.Iterator;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.metadata.ApplicationMetaData;
import org.jboss.metadata.AssemblyDescriptorMetaData;
import org.jboss.wsf.spi.deployment.SecurityRolesHandler;
import org.jboss.wsf.spi.deployment.UnifiedDeploymentInfo;
import org.jboss.wsf.spi.utils.DOMUtils;
import org.w3c.dom.Element;

/**
 * Generate a service endpoint deployment for EJB endpoints 
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 12-May-2006
 */
public class SecurityRolesHandlerEJB21 implements SecurityRolesHandler
{
   // logging support
   protected Logger log = Logger.getLogger(SecurityRolesHandlerEJB21.class);

   /** Add the roles from ejb-jar.xml to the security roles
    */
   public void addSecurityRoles(Element webApp, UnifiedDeploymentInfo udi)
   {
      // Fix: http://jira.jboss.org/jira/browse/JBWS-309
      ApplicationMetaData applMetaData = (ApplicationMetaData)udi.getAttachment(ApplicationMetaData.class);
      AssemblyDescriptorMetaData assemblyDescriptor = applMetaData.getAssemblyDescriptor();
      if (assemblyDescriptor != null)
      {
         Map securityRoles = assemblyDescriptor.getSecurityRoles();
         if (securityRoles != null)
         {
            Iterator it = securityRoles.keySet().iterator();
            while (it.hasNext())
            {
               Element securityRole = (Element)webApp.appendChild(DOMUtils.createElement("security-role"));
               Element roleName = (Element)securityRole.appendChild(DOMUtils.createElement("role-name"));
               roleName.appendChild(DOMUtils.createTextNode((String)it.next()));
            }
         }
      }
   }
}
