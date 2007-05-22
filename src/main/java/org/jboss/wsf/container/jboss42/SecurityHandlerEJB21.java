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

import org.dom4j.Element;
import org.jboss.metadata.ApplicationMetaData;
import org.jboss.metadata.AssemblyDescriptorMetaData;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.SecurityHandler;
import org.jboss.wsf.spi.metadata.j2ee.UnifiedApplicationMetaData;

/**
 * Generate a service endpoint deployment for EJB endpoints 
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 12-May-2006
 */
public class SecurityHandlerEJB21 implements SecurityHandler
{
   public void addSecurityDomain(Element jbossWeb, Deployment dep)
   {
      UnifiedApplicationMetaData appMetaData = dep.getContext().getAttachment(UnifiedApplicationMetaData.class);
      if (appMetaData == null)
         throw new IllegalStateException("Cannot obtain application meta data");

      String securityDomain = appMetaData.getSecurityDomain();
      if (securityDomain != null)
         jbossWeb.addElement("security-domain").addText("java:/jaas/" + securityDomain);
   }

   public void addSecurityRoles(Element webApp, Deployment dep)
   {
      // Fix: http://jira.jboss.org/jira/browse/JBWS-309
      ApplicationMetaData applMetaData = dep.getContext().getAttachment(ApplicationMetaData.class);
      AssemblyDescriptorMetaData assemblyDescriptor = applMetaData.getAssemblyDescriptor();
      if (assemblyDescriptor != null)
      {
         Map securityRoles = assemblyDescriptor.getSecurityRoles();
         if (securityRoles != null)
         {
            Iterator it = securityRoles.keySet().iterator();
            while (it.hasNext())
            {
               webApp.addElement("security-role").addElement("role-name").addText((String)it.next());
            }
         }
      }
   }
}
