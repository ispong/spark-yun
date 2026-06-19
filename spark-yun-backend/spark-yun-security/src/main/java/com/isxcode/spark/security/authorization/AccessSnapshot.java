package com.isxcode.spark.security.authorization;

import com.isxcode.spark.common.jpa.DataScopeContext;
import java.util.Set;

public record AccessSnapshot(String userId,String tenantId,boolean systemAdmin,boolean platformAdmin,boolean tenantAdmin,boolean normalAdmin,boolean workspaceAllPermissions,boolean apiAllPermissions,Set<String>permissions,Set<String>frontendPermissionCodes,Set<String>backendPermissionCodes,DataScopeContext.ResourceScope clusterScope,DataScopeContext.ResourceScope datasourceScope,DataScopeContext.ResourceScope fileScope){

public boolean hasTenantAccess(){

return tenantAdmin||normalAdmin||tenantId!=null;}

public boolean hasAllWorkspacePermissions(){

return tenantAdmin||normalAdmin||workspaceAllPermissions;}

public boolean hasAllApiPermissions(){

return tenantAdmin||normalAdmin||apiAllPermissions;}

public DataScopeContext.DataScope dataScope(){

return new DataScopeContext.DataScope(clusterScope,datasourceScope,fileScope);}}
