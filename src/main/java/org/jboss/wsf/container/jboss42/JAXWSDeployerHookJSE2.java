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
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.metadata.WebMetaData;

import javax.jws.WebService;
import javax.xml.ws.WebServiceProvider;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Heiko.Braun@jboss.com
 * @version $Revision$
 */
public class JAXWSDeployerHookJSE2 extends Phase2DeployerHookJSE
{
   /** Get the deployment type this deployer can handle
    */
   public Deployment.DeploymentType getDeploymentType()
   {
      return Deployment.DeploymentType.JAXWS_JSE;
   }


   /**
    * Reject JAX-RPC deployments
    */
   @Override
   public boolean isWebServiceDeployment(DeploymentInfo unit)
   {     
      WebservicesMetaData wsMetaData = getWebservicesMetaData(unit, "WEB-INF/webservices.xml");  // JAX-RPC artifact
      return (wsMetaData==null && super.isWebServiceDeployment(unit));
   }
}
