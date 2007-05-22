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

// $Id$

import java.security.Principal;

import org.jboss.security.SecurityAssociation;
import org.jboss.wsf.spi.invocation.SecurityAdaptor;

/**
 * A JBoss specific SecurityAssociationAdaptor 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 05-May-2006
 */
public class SecurityAdaptorImpl implements SecurityAdaptor
{
   public Principal getPrincipal()
   {
      return SecurityAssociation.getPrincipal();
   }
   
   public void setPrincipal(Principal pricipal)
   {
      SecurityAssociation.setPrincipal(pricipal);
   }

   public Object getCredential()
   {
      return SecurityAssociation.getCredential();
   }

   public void setCredential(Object credential)
   {
      SecurityAssociation.setCredential(credential);
   }
}
