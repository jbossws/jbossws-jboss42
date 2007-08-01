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
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;

/**
 * @deprecated
 */
public class DelegatingHandlerChainMetaData extends ServiceRefElement
{
   UnifiedHandlerChainMetaData delegate;

   public DelegatingHandlerChainMetaData()
   {
      delegate = new UnifiedHandlerChainMetaData();
   }

   public void setPortNamePattern(QName portNamePattern)
   {
      delegate.setPortNamePattern(portNamePattern);
   }

   public void setProtocolBindings(String protocolBindings)
   {
      delegate.setProtocolBindings(protocolBindings);
   }

   public void setServiceNamePattern(QName serviceNamePattern)
   {
      delegate.setServiceNamePattern(serviceNamePattern);
   }
   
}
