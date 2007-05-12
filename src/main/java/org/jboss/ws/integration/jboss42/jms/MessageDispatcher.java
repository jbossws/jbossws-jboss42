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
package org.jboss.ws.integration.jboss42.jms;

// $Id:MessageDispatcher.java 898 2006-09-05 08:23:03Z thomas.diesler@jboss.com $

import java.io.InputStream;
import java.rmi.RemoteException;

import javax.management.ObjectName;
import javax.xml.soap.SOAPMessage;

import org.jboss.ws.integration.ObjectNameFactory;

/**
 * A dispatcher for SOAPMessages 
 * 
 * @author Thomas.Diesler@jboss.org
 * @since 19-Feb-2006
 */
public interface MessageDispatcher
{
   public static final ObjectName OBJECT_NAME = ObjectNameFactory.create("jboss.ws:service=JMSTransportSupport");
   
   /** Dispatch the message to the underlying SOAP engine
    */
   SOAPMessage dipatchMessage(String fromName, Object targetImplBean, InputStream reqMessage) throws RemoteException;
   
   /** Dispatch the message to the underlying SOAP engine
    */
   SOAPMessage delegateMessage(String serviceID, InputStream reqMessage) throws RemoteException;
}