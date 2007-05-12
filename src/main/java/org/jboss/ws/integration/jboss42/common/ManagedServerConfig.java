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
package org.jboss.ws.integration.jboss42.common;

// $Id$

import java.util.ArrayList;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.jboss.logging.Logger;
import org.jboss.ws.integration.management.BasicServerConfig;

/**
 * A Service Endpoint Registry
 *
 * @author Thomas.Diesler@jboss.org
 * @since 04-May-2007
 */
public class ManagedServerConfig extends BasicServerConfig implements ManagedServerConfigMBean
{
   // provide logging
   private static final Logger log = Logger.getLogger(ManagedServerConfig.class);

   public void create() throws Exception
   {
      log.debug("WebServiceHost: " + getWebServiceHost());
      log.debug("WebServicePort: " + getWebServicePort());
      log.debug("WebServiceSecurePort: " + getWebServiceSecurePort());
      MBeanServer server = getMBeanServer();
      if (server != null)
      {
         server.registerMBean(this, OBJECT_NAME);
      }
   }

   public void destroy() throws Exception
   {
      MBeanServer server = getMBeanServer();
      if (server != null)
      {
         server.unregisterMBean(OBJECT_NAME);
      }
   }

   private MBeanServer getMBeanServer()
   {
      MBeanServer server = null;
      ArrayList servers = MBeanServerFactory.findMBeanServer(null);
      if (servers.size() > 0)
      {
         server = (MBeanServer)servers.get(0);
      }
      return server;
   }
}
