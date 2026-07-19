
        
export enum UserRole {
  Admin = "系统管理员",
  LeadAttorney = "主办律师",
  Assistant = "助理人员",
}

export enum UserStatus {
  Active = "启用",
  Disabled = "禁用",
}

export interface UserModel {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  status: UserStatus;
  team: string; // 所属团队/部门
  createdAt: string;
}

export interface RoleModel {
  roleId: string;
  roleName: UserRole | string;
  description: string;
  memberCount: number;
}

export interface PermissionModel {
  action: string; // e.g., 'create', 'read', 'update', 'delete'
  entity: string; // e.g., 'Case', 'Document', 'User'
  label: string;
}

export interface RolePermissionSetModel extends RoleModel {
  permissions: PermissionModel[];
}


export const MOCK_USERS: UserModel[] = [
  {
    id: "user_a1",
    name: "张伟 (主办律师)",
    email: "zhangwei.law@firm.com",
    role: UserRole.LeadAttorney,
    status: UserStatus.Active,
    team: "民事诉讼一组",
    createdAt: "2023-01-15",
  },
  {
    id: "user_a2",
    name: "李娜 (助理人员)",
    email: "li.na@firm.com",
    role: UserRole.Assistant,
    status: UserStatus.Active,
    team: "民事诉讼一组",
    createdAt: "2023-03-20",
  },
  {
    id: "user_b1",
    name: "王勇 (系统管理员)",
    email: "wangyong.admin@firm.com",
    role: UserRole.Admin,
    status: UserStatus.Active,
    team: "IT/管理部门",
    createdAt: "2022-10-01",
  },
  {
    id: "user_c1",
    name: "赵雷 (主办律师)",
    email: "zhao.lei@firm.com",
    role: UserRole.LeadAttorney,
    status: UserStatus.Disabled,
    team: "知识产权组",
    createdAt: "2023-05-10",
  },
];

export const MOCK_ROLES: RoleModel[] = [
    { roleId: "r01", roleName: UserRole.Admin, description: "拥有对系统功能和用户管理的完全权限。", memberCount: 1 },
    { roleId: "r02", roleName: UserRole.LeadAttorney, description: "负责案件的核心管理、文书生成和提醒设置，可查看所有自己负责或协办的案件。", memberCount: 2 },
    { roleId: "r03", roleName: UserRole.Assistant, description: "主要负责案件信息录入和辅助操作，只能查看被分配的案件详情。", memberCount: 3 },
    { roleId: "r04", roleName: "见习律师", description: "仅能查看公开案件列表，不能进行编辑或生成文书。", memberCount: 5 },
];

export const MOCK_ADMIN_PERMISSION_SET: RolePermissionSetModel = {
    roleId: "r01",
    roleName: UserRole.Admin,
    description: "拥有对系统功能和用户管理的完全权限。",
    memberCount: 1,
    permissions: [
        { action: "management", entity: "User", label: "用户账户管理" },
        { action: "management", entity: "Role_Permission", label: "角色与权限配置" },
        { action: "full_access", entity: "Case", label: "所有案件数据查看与编辑" },
        { action: "full_access", entity: "Document_Template", label: "文书模板库管理" },
        { action: "review", entity: "Audit_Log", label: "审计日志查看" },
    ],
};

export function getUserByRole(role: UserRole): UserModel[] {
    return MOCK_USERS.filter(user => user.role === role);
}

export function getUserById(userId: string): UserModel | undefined {
    return MOCK_USERS.find(user => user.id === userId);
}
        
      