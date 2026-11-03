export async function downloadTickets(status: string, query: string) {
  const response = await fetch(
    "/api/tickets/export?" + new URLSearchParams({ status, query }),
    { credentials: "include" },
  );
  if (!response.ok) {
    const result = await response.json().catch(() => ({ message: "导出失败" }));
    throw new Error(result.message || "导出失败");
  }
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement("a");
  link.href = url;
  link.download = "tickets.csv";
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
