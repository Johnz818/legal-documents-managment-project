
        
export interface LinkModel {
  id: string;
  label: string;
  iconName: string;
  url: string;
}

export interface NavigationSectionModel {
  title: string;
  links: LinkModel[];
}

export const mainNavigation: NavigationSectionModel[] = [
  {
    title: "核心业务",
    links: [
      { id: "case_management", label: "案件批量管理", iconName: "Gavel", url: "/case-bulk-management" },
      { id: "document_generation", label: "文书自动生成", iconName: "FileText", url: "/document-generation" },
      { id: "key_reminders", label: "关键环节提醒", iconName: "CalendarDays", url: "/reminder-dashboard" },
    ],
  },
  {
    title: "系统工具",
    links: [
      { id: "team_collaboration", label: "团队协作与权限", iconName: "Users", url: "/user-management" },
      { id: "audit_log", label: "日志与审计", iconName: "FileSearch", url: "/audit-log" },
    ],
  },
];

// Navigation links for specific entry pages (Hubs)
export const CaseManagementEntryLinks: LinkModel[] = [
  { id: "entry_list", label: "查看案件列表", iconName: "ListChecks", url: "/case-list" },
  { id: "entry_create", label: "手动创建案件", iconName: "Gavel", url: "/case-create" },
  { id: "entry_tags", label: "管理案件标签", iconName: "Tag", url: "/tag-management" },
];

export const DocumentGenerationEntryLinks: LinkModel[] = [
  { id: "entry_template", label: "文书模板管理", iconName: "LayoutTemplate", url: "/document-templates" },
  { id: "entry_generate", label: "发起文书生成", iconName: "FileSignature", url: "/document-generate/select" },
];

export const ReminderDashboardEntryLinks: LinkModel[] = [
  { id: "entry_calendar", label: "案件日历视图", iconName: "Calendar", url: "/reminder-calendar" },
  { id: "entry_notifications", label: "系统通知中心", iconName: "Bell", url: "/notification-center" },
];

export const UserManagementEntryLinks: LinkModel[] = [
    { id: "entry_users", label: "用户列表管理", iconName: "Users", url: "/user-list" },
    { id: "entry_roles", label: "角色与权限设置", iconName: "ShieldCheck", url: "/permission-roles" },
];
        
      