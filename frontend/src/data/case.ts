
        
import { MOCK_USERS } from './user';

export enum CaseStage {
  PendingFiling = "立案中",
  TrialPreparation = "审理准备阶段",
  InProgress = "审理中",
  ClosedPendingAppeal = "已判决(上诉期内)",
  Appeal = "上诉审理中",
  Adjudicated = "已判决(生效)",
  Enforcement = "执行中",
  Archived = "已归档",
}

export interface CaseTagModel {
  id: string;
  name: string;
  color: string; // Use Hex or Tailwind Color Class
}

export interface CaseModel {
  id: string;
  caseNumber: string; // 案号
  courtName: string; // 法院名称
  caseCause: string; // 案由 (e.g., 合同纠纷, 知识产权)
  caseStage: CaseStage; // 案件阶段
  plaintiff: string; // 原告/申请人
  defendant: string; // 被告/被申请人
  leadAttorneyId: string; // 主办律师ID
  coAttorneysIds: string[]; // 协办律师ID列表
  filingDate: string | null; // 立案时间 (YYYY-MM-DD)
  hearingDate: string | null; // 开庭时间 (YYYY-MM-DD)
  judgmentDate: string | null; // 判决时间 (YYYY-MM-DD)
  tags: CaseTagModel[];
  description?: string;
}

export interface CaseSummaryModel extends Omit<CaseModel, 'description' | 'coAttorneysIds'>{
    leadAttorneyName: string; 
}


export const MOCK_CASE_TAGS: CaseTagModel[] = [
  { id: "t1", name: "紧急", color: "bg-red-500" },
  { id: "t2", name: "重大", color: "bg-yellow-500" },
  { id: "t3", name: "再审中", color: "bg-purple-500" },
  { id: "t4", name: "合同类", color: "bg-blue-500" },
  { id: "t5", name: "已归档", color: "bg-gray-500" },
];

const ATTORNEY_ZHANG = MOCK_USERS.find(u => u.name.includes("张伟")) || MOCK_USERS[0];
const ATTORNEY_ZHAO = MOCK_USERS.find(u => u.name.includes("赵雷")) || MOCK_USERS[3];
const ASSISTANT_LI = MOCK_USERS.find(u => u.name.includes("李娜")) || MOCK_USERS[1];


export const MOCK_CASES: CaseModel[] = [
  {
    id: "C2024001",
    caseNumber: "(2024)京01民初188号",
    courtName: "北京市第一中级人民法院",
    caseCause: "房屋买卖合同纠纷",
    caseStage: CaseStage.TrialPreparation,
    plaintiff: "北京方圆科技有限公司",
    defendant: "李铭",
    leadAttorneyId: ATTORNEY_ZHANG.id,
    coAttorneysIds: [ASSISTANT_LI.id],
    filingDate: "2024-05-10",
    hearingDate: "2024-08-25",
    judgmentDate: null,
    tags: [MOCK_CASE_TAGS[0], MOCK_CASE_TAGS[3]], // 紧急, 合同类
    description: "涉及高额违约金的房屋买卖合同纠纷，需尽快准备证据材料并提交，预计开庭时间在8月底。",
  },
  {
    id: "C2024002",
    caseNumber: "(2024)沪02知民终23号",
    courtName: "上海市第二中级人民法院",
    caseCause: "商标侵权及不正当竞争",
    caseStage: CaseStage.InProgress,
    plaintiff: "上海创新设计有限公司",
    defendant: "深圳市快仿电子厂",
    leadAttorneyId: ATTORNEY_ZHANG.id,
    coAttorneysIds: [],
    filingDate: "2024-06-01",
    hearingDate: "2024-07-20",
    judgmentDate: null,
    tags: [MOCK_CASE_TAGS[1]], // 重大
    description: "涉及知名品牌商标侵权，影响重大，已完成一审庭审，等待二审开庭。",
  },
  {
    id: "C2023010",
    caseNumber: "(2023)粤03民初99号",
    courtName: "深圳市福田区人民法院",
    caseCause: "劳动争议",
    caseStage: CaseStage.Adjudicated,
    plaintiff: "王芳",
    defendant: "南方人力资源有限公司",
    leadAttorneyId: ATTORNEY_ZHAO.id,
    coAttorneysIds: [],
    filingDate: "2023-11-01",
    hearingDate: "2024-01-20",
    judgmentDate: "2024-03-05",
    tags: [MOCK_CASE_TAGS[4]], // 已归档
    description: "案件已判决，公司胜诉，判决已生效。案件结案归档。",
  },
  {
    id: "C2024004",
    caseNumber: "(2024)苏01民再10号",
    courtName: "江苏省高级人民法院",
    caseCause: "民间借贷纠纷",
    caseStage: CaseStage.Appeal,
    plaintiff: "周立",
    defendant: "孙健",
    leadAttorneyId: ATTORNEY_ZHANG.id,
    coAttorneysIds: [ATTORNEY_ZHAO.id],
    filingDate: "2024-04-15",
    hearingDate: "2024-07-10",
    judgmentDate: "2024-06-01", // 一审判决时间，现处于再审
    tags: [MOCK_CASE_TAGS[2]], // 再审中
    description: "该案当事人不服一审判决提起上诉，二审正在审理中，原计划7月10日开庭已延期。",
  },
];

export function getCaseById(caseId: string): CaseModel | undefined {
  return MOCK_CASES.find(c => c.id === caseId);
}

export function getCaseSummaryList(): CaseSummaryModel[] {
    return MOCK_CASES.map(c => ({
        ...c,
        leadAttorneyName: MOCK_USERS.find(u => u.id === c.leadAttorneyId)?.name || '未知',
    }));
}


// Mock data/info for batch import page
export interface TemplateInfoModel {
    name: string;
    description: string;
    fields: string;
    downloadUrl: string;
    exampleImageUrl: string;
}

export const IMPORT_TEMPLATE_INFO: TemplateInfoModel = {
    name: "案件批量导入模板 v1.0",
    description: "请下载标准Excel模板，并根据要求填写案件核心字段。必填字段包括：案号、法院名称、原告、被告、案由、案件阶段(请参考枚举值)。",
    fields: "案号, 法院名称, 原告, 被告, 案由, 案件阶段, 主办律师工号, 立案时间, 开庭时间, 判决时间...",
    downloadUrl: "/api/download/case-template.xlsx",
    exampleImageUrl: "https://spark-builder.s3.cn-north-1.amazonaws.com.cn/image/2025/12/14/acfb0f08-b582-471b-b1d6-4f14f9d455ab.png",
};

export interface ImportResultModel {
    totalRecords: number;
    successCount: number;
    failureCount: number;
    failedReasons: string[];
}

// Mock function for simulating import
export function simulateCaseImport(file: File): ImportResultModel {
    console.log(`Simulating import for file: ${file.name}`);
    return {
        totalRecords: 150,
        successCount: 145,
        failureCount: 5,
        failedReasons: [
            "第5行：案号重复",
            "第12行：主办律师工号不存在",
            "第45行：案件阶段字段值不规范",
        ],
    };
}
