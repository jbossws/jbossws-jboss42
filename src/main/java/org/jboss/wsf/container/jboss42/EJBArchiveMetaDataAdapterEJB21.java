/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import org.jboss.metadata.BeanMetaData;
import org.jboss.metadata.EjbPortComponentMetaData;
import org.jboss.metadata.MessageDrivenMetaData;
import org.jboss.metadata.SessionMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBSecurityMetaData;
import org.jboss.wsf.spi.metadata.j2ee.MDBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.SLSBMetaData;

/**
 * Build container independent application meta data 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 05-May-2006
 */
public class EJBArchiveMetaDataAdapterEJB21 extends EJBArchiveMetaDataAdapter
{
   @Override
   protected EJBMetaData buildUnifiedBeanMetaData(BeanMetaData bmd)
   {
      EJBMetaData ubmd = null;
      if (bmd instanceof SessionMetaData)
      {
         ubmd = new SLSBMetaData();
      }
      else if (bmd instanceof MessageDrivenMetaData)
      {
         ubmd = new MDBMetaData();
         ((MDBMetaData)ubmd).setDestinationJndiName(((MessageDrivenMetaData)bmd).getDestinationJndiName());
      }

      if (ubmd != null)
      {
         ubmd.setEjbName(bmd.getEjbName());
         ubmd.setEjbClass(bmd.getEjbClass());
         ubmd.setServiceEndpointInterface(bmd.getServiceEndpoint());
         ubmd.setHome(bmd.getHome());
         ubmd.setLocalHome(bmd.getLocalHome());
         ubmd.setJndiName(bmd.getJndiName());
         ubmd.setLocalJndiName(bmd.getLocalJndiName());

         EjbPortComponentMetaData pcmd = bmd.getPortComponent();
         if (pcmd != null)
         {
            ubmd.setPortComponentName(pcmd.getPortComponentName());
            ubmd.setPortComponentURI(pcmd.getPortComponentURI());
            EJBSecurityMetaData smd = new EJBSecurityMetaData();
            smd.setAuthMethod(pcmd.getAuthMethod());
            smd.setTransportGuarantee(pcmd.getTransportGuarantee());
            smd.setSecureWSDLAccess(pcmd.getSecureWSDLAccess());
            ubmd.setSecurityMetaData(smd);
         }
      }
      return ubmd;
   }
}
