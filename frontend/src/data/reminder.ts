
        
import { CaseStage } from './case';

export enum ReminderType {
    KeyDate = "关键日期提醒",
    Custom = "自定义事项",
    SystemAlert = "系统预警提醒",
}

export enum ReminderMethod {
    System = "系统通知",
    Email = "邮件通知",
    Both = "系统与邮件",
}

export interface ReminderModel {
  reminderId: string;
  caseId: string;
  type: ReminderType;
  title: string;
  targetDate: string; // YYYY-MM-DD HH:MM
  isCompleted: boolean;
  linkedStage: CaseStage | 'N/A';
  reminderMethod: ReminderMethod[];
}

export interface NotificationModel {
    notificationId: string;
    type: 'alert' | 'info' | 'success';
    message: string;
    timestamp: string;
    isRead: boolean;
    caseId?: string; // Links to the relevant case
}

export interface CalendarEventModel {
    id: string;
    caseId: string;
    title: string;
    start: string; // ISO Date string
    color: string; // Calendar display color
    reminderType: ReminderType;
}

export interface ReminderSummaryModel {
    todayReminders: number;
    upcomingReminders: number;
    overdueReminders: number;
}


// --- MOCK DATA ---
export const MOCK_REMINDERS: ReminderModel[] = [
  {
    reminderId: "R001",
    caseId: "C2024001",
    type: ReminderType.KeyDate,
    title: "开庭提醒",
    targetDate: "2024-08-25 09:30",
    isCompleted: false,
    linkedStage: CaseStage.TrialPreparation,
    reminderMethod: [ReminderMethod.System, ReminderMethod.Email],
  },
  {
    reminderId: "R002",
    caseId: "C2024001",
    type: ReminderType.Custom,
    title: "证据材料提交截止日期",
    targetDate: "2024-07-30 17:00",
    isCompleted: false,
    linkedStage: CaseStage.TrialPreparation,
    reminderMethod: [ReminderMethod.System],
  },
  {
    reminderId: "R003",
    caseId: "C2024002",
    type: ReminderType.KeyDate,
    title: "二审开庭提醒",
    targetDate: "2024-07-20 14:00",
    isCompleted: false,
    linkedStage: CaseStage.InProgress,
    reminderMethod: [ReminderMethod.System],
  },
  {
    reminderId: "R004",
    caseId: "C2023010",
    type: ReminderType.KeyDate,
    title: "上诉期截止 (已过期)",
    targetDate: "2024-03-20 23:59",
    isCompleted: true,
    linkedStage: CaseStage.Adjudicated,
    reminderMethod: [ReminderMethod.System],
  },
];

export const MOCK_NOTIFICATIONS: NotificationModel[] = [
    {
        notificationId: "N001",
        type: 'alert',
        message: "案件 C2024001 的开庭日期（8月25日）即将到来，请确认准备进度。",
        timestamp: "2025-12-14 09:00",
        isRead: false,
        caseId: "C2024001",
    },
    {
        notificationId: "N002",
        type: 'info',
        message: "文书模板 T003 已被修改。",
        timestamp: "2025-12-13 18:30",
        isRead: true,
    },
    {
        notificationId: "N003",
        type: 'success',
        message: "批量导入任务成功完成，共导入 145 个案件。",
        timestamp: "2025-12-13 14:15",
        isRead: true,
    },
];

export const MOCK_REMINDER_SUMMARY: ReminderSummaryModel = {
    todayReminders: 1,
    upcomingReminders: 5,
    overdueReminders: 0,
};

export const MOCK_CALENDAR_EVENTS: CalendarEventModel[] = MOCK_REMINDERS
    .filter(r => !r.isCompleted)
    .map(r => ({
        id: r.reminderId,
        caseId: r.caseId,
        title: `${r.title} (${r.caseId})`,
        start: r.targetDate,
        color: r.type === ReminderType.KeyDate ? '#e35f5f' : '#3498db', // Red for key dates, Blue for custom
        reminderType: r.type,
    }));
        
      