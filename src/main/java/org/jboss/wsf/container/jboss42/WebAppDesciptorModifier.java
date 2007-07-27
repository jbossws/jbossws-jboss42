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
import org.dom4j.Document;

/**
 * Modifies the web app according to the stack requirements.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 19-May-2007
 */
public interface WebAppDesciptorModifier
{
   final String SERVLET_CONTEXT_LISTENER = "org.jboss.ws.webapp.ServletContextListener";
   final String CONTEXT_PARAMETER_MAP = "org.jboss.ws.webapp.ContextParameterMap";
   final String SERVLET_CLASS = "org.jboss.ws.webapp.ServletClass";

   RewriteResults modifyDescriptor(Deployment dep, Document webXml) throws ClassNotFoundException;
}
