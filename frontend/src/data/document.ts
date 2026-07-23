
        
import { type CaseModel, MOCK_CASES, CaseStage } from './case';
import { MOCK_USERS, type UserModel } from './user';

export enum TemplateType {
  Preset = "系统预设",
  Custom = "用户自定义",
}

export interface TemplateVariableModel {
    variableName: string; // e.g., {{原告}}
    sourceField: keyof CaseModel | keyof UserModel | 'systemTime';
    description: string;
}

export interface DocumentTemplateModel {
  templateId: string;
  name: string;
  type: TemplateType;
  description: string;
  contentTemplate: string; // Placeholder content for the template body
  lastModified: string;
  creator?: string; // User ID
}

export interface DocumentModel {
  documentId: string;
  caseId: string;
  templateId: string;
  docName: string;
  stage: CaseStage; // 生成时的案件阶段
  generatedDate: string;
  content: string; // Actual generated and possibly edited content (HTML/Text)
}

// --- MOCK DATA ---

export const MOCK_TEMPLATE_VARIABLES: TemplateVariableModel[] = [
  { variableName: "{{案号}}", sourceField: "caseNumber", description: "案件的唯一标识码" },
  { variableName: "{{法院}}", sourceField: "courtName", description: "受理案件的法院全称" },
  { variableName: "{{原告}}", sourceField: "plaintiff", description: "原告或申请人姓名/单位" },
  { variableName: "{{被告}}", sourceField: "defendant", description: "被告或被申请人姓名/单位" },
  { variableName: "{{案由}}", sourceField: "caseCause", description: "案件纠纷类型" },
  { variableName: "{{主办律师}}", sourceField: "name", description: "当前案件主办律师姓名" },
  { variableName: "{{开庭时间}}", sourceField: "hearingDate", description: "案件开庭日期" },
  { variableName: "{{当前日期}}", sourceField: "systemTime", description: "文书生成时的系统日期" },
];

const PROOF_OF_AGENCY_CONTENT = 
    "致{{法院}}：\n\n兹委托{{主办律师}}为我方在贵院审理的{{案号}}（{{案由}}）一案中担任诉讼代理人。代理权限详情见附件。特此委托。\n\n委托人：{{原告}}\n{{当前日期}}";

const PETITION_CONTENT = 
    "原告：{{原告}}\n被告：{{被告}}\n\n诉讼请求：\n1. 请求法院依法判令被告立即支付拖欠款项人民币XX元。\n2. 请求判令被告承担原告诉讼费、律师费。\n\n事实与理由：\n原被告于X年X月X日签订了XXX合同，被告至今未履行支付义务，已严重违约。根据《中华人民共和国民法典》相关规定，特向贵院提起诉讼，望支持原告诉讼请求。\n\n此致\n{{法院}}\n\n具状人：{{主办律师}}\n{{当前日期}}";

export const MOCK_DOCUMENT_TEMPLATES: DocumentTemplateModel[] = [
  {
    templateId: "T001",
    name: "民事起诉状 (标准版)",
    type: TemplateType.Preset,
    description: "适用于常见民事纠纷的起诉文书模板。",
    contentTemplate: PETITION_CONTENT,
    lastModified: "2023-11-01",
  },
  {
    templateId: "T002",
    name: "授权委托书 (通用)",
    type: TemplateType.Preset,
    description: "用于授权律师代理诉讼的委托书。",
    contentTemplate: PROOF_OF_AGENCY_CONTENT,
    lastModified: "2023-10-25",
  },
  {
    templateId: "T003",
    name: "劳动仲裁答辩状 (自定义)",
    type: TemplateType.Custom,
    description: "针对劳动争议仲裁申请的答辩书模板，由用户A创建。",
    contentTemplate: "申请人：{{原告}}\n被申请人：{{被告}}\n\n...[自定义答辩内容]...",
    lastModified: "2024-03-15",
    creator: MOCK_USERS[0].id,
  },
];

export const MOCK_DOCUMENTS: DocumentModel[] = [
  {
    documentId: "D001",
    caseId: "C2024001",
    templateId: "T001",
    docName: "房屋买卖合同纠纷起诉状 v1",
    stage: CaseStage.TrialPreparation,
    generatedDate: "2024-05-11 10:30",
    content: PETITION_CONTENT.replace("{{原告}}", MOCK_CASES[0].plaintiff).replace("{{被告}}", MOCK_CASES[0].defendant).replace("{{法院}}", MOCK_CASES[0].courtName).replace("{{案号}}", MOCK_CASES[0].caseNumber).replace("{{案由}}", MOCK_CASES[0].caseCause).replace("{{主办律师}}", MOCK_USERS[0].name).replace("{{当前日期}}", "2024年5月11日"),
  },
  {
    documentId: "D002",
    caseId: "C2024002",
    templateId: "T002",
    docName: "商标侵权案 - 授权委托书",
    stage: CaseStage.InProgress,
    generatedDate: "2024-06-05 14:00",
    content: PROOF_OF_AGENCY_CONTENT.replace("{{原告}}", MOCK_CASES[1].plaintiff).replace("{{案号}}", MOCK_CASES[1].caseNumber).replace("{{案由}}", MOCK_CASES[1].caseCause).replace("{{法院}}", MOCK_CASES[1].courtName).replace("{{主办律师}}", MOCK_USERS[0].name).replace("{{当前日期}}", "2024年6月5日"),
  },
];

export function generateDocumentPreview(caseId: string, templateId: string): DocumentModel | null {
    const caseData = MOCK_CASES.find(c => c.id === caseId);
    const template = MOCK_DOCUMENT_TEMPLATES.find(t => t.templateId === templateId);

    if (!caseData || !template) return null;

    const leadAttorney = MOCK_USERS.find(u => u.id === caseData.leadAttorneyId);
    const currentDate = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' });

    let content = template.contentTemplate;

    // Simple placeholder replacement simulation
    content = content.replace(/\{\{案号\}\}/g, caseData.caseNumber);
    content = content.replace(/\{\{法院\}\}/g, caseData.courtName);
    content = content.replace(/\{\{原告\}\}/g, caseData.plaintiff);
    content = content.replace(/\{\{被告\}\}/g, caseData.defendant);
    content = content.replace(/\{\{案由\}\}/g, caseData.caseCause);
    content = content.replace(/\{\{主办律师\}\}/g, leadAttorney ? leadAttorney.name : 'N/A');
    content = content.replace(/\{\{开庭时间\}\}/g, caseData.hearingDate || '待定');
    content = content.replace(/\{\{当前日期\}\}/g, currentDate);
    content = content.replace('X年X月X日', '2024年5月1日'); // Mock context specific date

    return {
        documentId: 'D' + Date.now().toString(),
        caseId: caseId,
        templateId: templateId,
        docName: `${caseData.caseNumber}-${template.name}-初稿`,
        stage: caseData.caseStage,
        generatedDate: new Date().toISOString().substring(0, 16).replace('T', ' '),
        content: content,
    };
}
