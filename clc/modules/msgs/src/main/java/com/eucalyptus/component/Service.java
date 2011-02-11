/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.component;

import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.component.auth.SystemCredentialProvider;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.HasParent;
import com.eucalyptus.util.NetworkUtil;
import edu.emory.mathcs.backport.java.util.Arrays;

public class Service implements ComponentInformation, HasParent<Component>, HasFullName<Service> {
  public static String               LOCAL_HOSTNAME = "@localhost";
  private final String               name;
  private final ServiceEndpoint      endpoint;
  private final Dispatcher           dispatcher;
  private final ServiceConfiguration serviceConfiguration;
  private final ComponentId          id;
  private final Component.State      state          = State.ENABLED/** ASAP:FIXME:GRZE **/
                                                    ;
  private final FullName             fullName;
  
  public final ServiceId getServiceId( ) {
    return new ServiceId( ) {
      {
        this.setUuid( serviceConfiguration.getId( ) );
        this.setPartition( serviceConfiguration.getPartition( ) );
        this.setName( serviceConfiguration.getName( ) );
        this.setType( serviceConfiguration.getComponentId( ).getName( ) );
        this.setUri( serviceConfiguration.getUri( ) );
      }
    };
  }
  
  public Service( ComponentId id, ServiceConfiguration serviceConfig ) {
    this.id = id;
    this.serviceConfiguration = serviceConfig;
    this.fullName = this.id.getMyFullName( this.serviceConfiguration );
    if ( "cluster".equals( id.getName( ) ) && Components.lookup( "eucalyptus" ).isLocal( ) ) /*ASAP: fix this disgusting hack.*/{
      this.name = id.getName( ) + "@" + serviceConfig.getHostName( );
      URI uri = this.id.makeRemoteUri( serviceConfig.getHostName( ), serviceConfig.getPort( ) );
      this.endpoint = new ServiceEndpoint( this, false, uri );
    } else if ( serviceConfig.isLocal( ) ) {
      URI uri = this.id.getLocalEndpointUri( );
      this.name = id.getName( ) + LOCAL_HOSTNAME;
      this.endpoint = new ServiceEndpoint( this, true, uri );
    } else {
      Boolean local = false;
      try {
        if ( serviceConfig.getHostName( ) != null ) {
          local = NetworkUtil.testLocal( serviceConfig.getHostName( ) );
        } else {
          local = true;
        }
      } catch ( Exception e ) {
        local = true;
      }
      URI uri = null;
      if ( !local ) {
        this.name = id.getName( ) + "@" + serviceConfig.getHostName( );
        uri = this.id.makeRemoteUri( serviceConfig.getHostName( ), serviceConfig.getPort( ) );
      } else {
        this.name = id.getName( ) + LOCAL_HOSTNAME;
        uri = this.id.getLocalEndpointUri( );
      }
      this.endpoint = new ServiceEndpoint( this, local, uri );
    }
    this.dispatcher = DispatcherFactory.build( Components.lookup( id ), this );
  }
  
  public Boolean isLocal( ) {
    return this.endpoint.isLocal( );
  }
  
  public KeyPair getKeys( ) {
    return SystemCredentialProvider.getCredentialProvider( this.id ).getKeyPair( );
  }
  
  public X509Certificate getCertificate( ) {
    return SystemCredentialProvider.getCredentialProvider( this.id ).getCertificate( );
  }
  
  public URI getUri( ) {
    return this.endpoint.get( );
  }
  
  public String getHost( ) {
    return this.endpoint.get( ).getHost( );
  }
  
  public Integer getPort( ) {
    return this.endpoint.get( ).getPort( );
  }
  
  public String getName( ) {
    return this.name;
  }
  
  public ServiceEndpoint getEndpoint( ) {
    return this.endpoint;
  }
  
  public Dispatcher getDispatcher( ) {
    return this.dispatcher;
  }
  
  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   * @param that
   * @return
   */
  @Override
  public int compareTo( Service that ) {
    if ( this.getServiceConfiguration( ).getPartition( ).equals( that.getServiceConfiguration( ).getPartition( ) ) ) {
      if ( that.getState( ).ordinal( ) == this.getState( ).ordinal( ) ) {
        return this.getName( ).compareTo( that.getName( ) );
      } else {
        return that.getState( ).ordinal( ) - this.getState( ).ordinal( );
      }
    } else {
      return this.getServiceConfiguration( ).getPartition( ).compareTo( that.getServiceConfiguration( ).getPartition( ) );
    }
  }
  
  /**
   * @return the service configuration
   */
  public ServiceConfiguration getServiceConfiguration( ) {
    return this.serviceConfiguration;
  }
  
  /**
   * @return the parent
   */
  public Component getParent( ) {
    return Components.lookup( this.id );
  }
  
  /**
   * @see java.lang.Object#toString()
   * @return
   */
  @Override
  public String toString( ) {
    return String.format( "Service %s name=%s endpoint=%s\nService %s name=%s serviceConfiguration=%s\nService %s name=%s keys=%s",
                          this.id, this.name, this.endpoint,
                          this.id, this.name, this.serviceConfiguration );
  }
  
  /**
   * @return the state
   */
  public Component.State getState( ) {
    if ( this.serviceConfiguration.isLocal( ) ) {
      return this.getParent( ).getState( );
    } else {
      return this.state;
    }
  }
  
  /** ASAP:FIXME:GRZE **/
  public List<String> getDetails( ) {
    return Arrays.asList( this.toString( ).split( "\n" ) );
  }
  
  /**
   * TODO: DOCUMENT
   * 
   * @see com.eucalyptus.util.HasFullName#getPartition()
   * @return
   */
  @Override
  public String getPartition( ) {
    return this.fullName.getPartition( );
  }
  
  @Override
  public FullName getFullName( ) {
    return this.fullName;
  }
  
}
