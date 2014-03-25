/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.web.SecurityNamedServicePanelInfo;

/**
 * Extension point for role service configuration panels.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class RoleServicePanelInfo 
    <C extends SecurityRoleServiceConfig, T extends RoleServicePanel<C>>
    extends SecurityNamedServicePanelInfo<C,T>{

}
