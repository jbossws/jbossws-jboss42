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

import java.util.ArrayList;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.ejb3.Ejb3ModuleMBean;
import org.jboss.ejb3.EJBContainer;
import org.jboss.ejb3.metamodel.EnterpriseBean;
import org.jboss.ejb3.mdb.MessagingContainer;
import org.jboss.ejb3.stateless.StatelessContainer;
import org.jboss.logging.Logger;
import org.jboss.metadata.ActivationConfigPropertyMetaData;
import org.jboss.mx.util.MBeanProxy;
import org.jboss.mx.util.MBeanProxyCreationException;
import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.WSFDeploymentException;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;
import org.jboss.wsf.spi.metadata.j2ee.EJBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.MDBMetaData;
import org.jboss.wsf.spi.metadata.j2ee.SLSBMetaData;
import org.jboss.wsf.spi.annotation.WebContext;

/**
 * Build container independent application meta data 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 14-Apr-2007
 */
public class EJBArchiveMetaDataAdapterEJB3
{
   // logging support
   private static Logger log = Logger.getLogger(EJBArchiveMetaDataAdapterEJB3.class);

   public static final String DEPLOYED_OBJECT = "org.jboss.ws.ejb3.deployed.object";

   public EJBArchiveMetaData buildUnifiedApplicationMetaData(Deployment dep)
   {
      EJBArchiveMetaData appMetaData = null;

      ObjectName oname = (ObjectName)dep.getProperty(DEPLOYED_OBJECT);

      // jboss.j2ee:service=EJB3,module=some-ejb3.jar
      if (oname != null && oname.getDomain().equals("jboss.j2ee") && "EJB3".equals(oname.getKeyProperty("service")))
      {
         appMetaData = new EJBArchiveMetaData();

         Ejb3ModuleMBean ejb3Module = getEJB3Module(oname);

         ArrayList<EJBMetaData> beans = new ArrayList<EJBMetaData>();
         for (Object container : ejb3Module.getContainers().values())
         {
            if (container instanceof StatelessContainer)
            {
               StatelessContainer slc = (StatelessContainer)container;
               SLSBMetaData usmd = new SLSBMetaData();
               usmd.setEjbName(slc.getEjbName());
               usmd.setEjbClass(slc.getBeanClassName());
               beans.add(usmd);

               buildWebServiceMetaData(appMetaData, slc);
            }
            else if (container instanceof MessagingContainer)
            {
               MessagingContainer mdb = (MessagingContainer)container;
               MDBMetaData umdb = new MDBMetaData();
               umdb.setEjbName(mdb.getEjbName());
               umdb.setEjbClass(mdb.getBeanClassName());
               Map props = mdb.getActivationConfigProperties();
               if (props != null)
               {
                  ActivationConfigPropertyMetaData destProp = (ActivationConfigPropertyMetaData)props.get("destination");
                  if (destProp != null)
                  {
                     String destination = destProp.getValue();
                     umdb.setDestinationJndiName(destination);
                  }
               }
               beans.add(umdb);

               buildWebServiceMetaData(appMetaData, mdb);
            }
         }

         appMetaData.setEnterpriseBeans(beans);
      }
      return appMetaData;
   }

   private void buildWebServiceMetaData(EJBArchiveMetaData appMetaData, EJBContainer container)
   {
      WebContext webContext = (WebContext)container.resolveAnnotation(WebContext.class);
      if(webContext!=null)
      {
         appMetaData.setWebServiceContextRoot(webContext.contextRoot());         
      }
   }

   static Ejb3ModuleMBean getEJB3Module(ObjectName objectName)
   {
      Ejb3ModuleMBean ejb3Module;
      try
      {
         MBeanServer server = MBeanServerLocator.locateJBoss();
         ejb3Module = (Ejb3ModuleMBean)MBeanProxy.get(Ejb3ModuleMBean.class, objectName, server);
         if (ejb3Module == null)
            throw new WSFDeploymentException("Cannot obtain EJB3 module: " + objectName);

         return ejb3Module;
      }
      catch (MBeanProxyCreationException ex)
      {
         throw new WSFDeploymentException("Cannot obtain proxy to EJB3 module");
      }
   }
}
