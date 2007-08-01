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
package org.jboss.wsf.container.jboss42.serviceref;

// $Id: UnifiedServiceRefMetaData.java 4044 2007-08-01 08:23:22Z thomas.diesler@jboss.com $

import javax.xml.namespace.QName;

import org.jboss.ws.integration.ServiceRefElement;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedPortComponentRefMetaData;

/**
 * @deprecated
 */
public class DelegatingPortComponentRefMetaData extends ServiceRefElement
{
   UnifiedPortComponentRefMetaData delegate;

   public DelegatingPortComponentRefMetaData(DelegatingServiceRefMetaData ref)
   {
      delegate = new UnifiedPortComponentRefMetaData(ref.delegate);
   }

   public void setConfigFile(String configFile)
   {
      delegate.setConfigFile(configFile);
   }

   public void setConfigName(String configName)
   {
      delegate.setConfigName(configName);
   }

   public void setEnableMTOM(Boolean enableMTOM)
   {
      delegate.setEnableMTOM(enableMTOM);
   }

   public void setPortComponentLink(String portComponentLink)
   {
      delegate.setPortComponentLink(portComponentLink);
   }

   public void setPortQName(QName portQName)
   {
      delegate.setPortQName(portQName);
   }

   public void setServiceEndpointInterface(String serviceEndpointInterface)
   {
      delegate.setServiceEndpointInterface(serviceEndpointInterface);
   }

   public void addCallProperty(DelegatingCallPropertyMetaData data)
   {
      delegate.addCallProperty(data.delegate);
   }

   public void addStubProperty(DelegatingStubPropertyMetaData data)
   {
      delegate.addStubProperty(data.delegate);
   }
}
