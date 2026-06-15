UPDATE sy_user
SET role_code = 'PLATFORM_ADMIN'
WHERE platform_admin = TRUE
  AND role_code != 'PLATFORM_SUPER_ADMIN';

UPDATE sy_user
SET role_code = 'PLATFORM_MEMBER'
WHERE role_code = 'TENANT_MEMBER';
