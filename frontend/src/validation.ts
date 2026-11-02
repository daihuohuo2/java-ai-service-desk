export function validateTicket(input: {
  orderId: string;
  kind: string;
  subject: string;
  description: string;
}): string {
  if (!input.subject.trim() || !input.description.trim())
    return "标题和问题描述不能只填写空格";
  if (input.subject.length > 120 || input.description.length > 2000)
    return "标题最多 120 字，描述最多 2000 字";
  if (!["QUESTION", "REFUND", "EXCHANGE", "REPAIR"].includes(input.kind))
    return "请选择有效的服务类型";
  if (input.kind !== "QUESTION" && !input.orderId)
    return "售后申请需要选择关联订单";
  return "";
}
