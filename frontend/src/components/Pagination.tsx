export function Pagination({
  page,
  pages,
  total,
  size,
  onPage,
  onSize,
}: {
  page: number;
  pages: number;
  total: number;
  size: number;
  onPage: (page: number) => void;
  onSize: (size: number) => void;
}) {
  return (
    <div className="pagination" aria-label="分页">
      <span>
        共 {total} 条 · 第 {page} / {Math.max(1, pages)} 页
      </span>
      <label>
        每页
        <select
          aria-label="每页条数"
          value={size}
          onChange={(event) => onSize(Number(event.target.value))}
        >
          {[1, 5, 10, 20, 50].map((value) => (
            <option key={value} value={value}>
              {value}
            </option>
          ))}
        </select>
      </label>
      <button
        className="secondary"
        disabled={page <= 1}
        onClick={() => onPage(page - 1)}
      >
        上一页
      </button>
      <button
        className="secondary"
        disabled={page >= pages}
        onClick={() => onPage(page + 1)}
      >
        下一页
      </button>
    </div>
  );
}
