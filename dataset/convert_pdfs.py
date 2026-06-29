from pymupdf4llm import to_markdown
import os
import re

def clean_md(md: str) -> str:
    # 1) Loại dòng chỉ là số (page number)
    md = re.sub(r'(?m)^\s*\d+\s*$', '', md)

    # 2) Xoá <br>, ký tự đặc biệt không cần thiết, nhưng giữ dấu tick ✓ và bảng
    md = md.replace('<br>', '')
    md = re.sub(r'[^\w\s\.\,\;\:\!\?\(\)\[\]\{\}\-\=\+\\/%%€₫¥₩₽₹“”"\'’‘–—•✓×°°C°F→←↑↓°±§≠≥≤≡≈…|‖·◦▪▫🔹🔸]+', '', md)

    # 3) Xử lý xuống dòng thừa và page break
    md = md.replace('\f', '\n')
    md = re.sub(r'\n{3,}', '\n\n', md)

    # 4) Loại header/footer bằng cách xoá các dòng lặp đầu/cuối (giả định dưới 5 dòng đầu/cuối)
    lines = md.splitlines()
    top_lines = lines[:5]
    bottom_lines = lines[-5:]
    common_headers = set(top_lines) & set(bottom_lines)
    lines = [line for line in lines if line.strip() not in common_headers]

    return '\n'.join(lines)

def fix_table_column_headers(md: str) -> str:
    # Nếu bảng có tiêu đề dạng | Col1 | Col2 | -> loại bỏ các "ColX" nếu không có nội dung thật
    def remove_fake_column_titles(match):
        header = match.group(1)
        if "Col" in header:
            # chỉ giữ lại cột có tên thật (chữ có dấu)
            header_parts = header.strip().split("|")
            new_header = '|'.join(part if re.search(r'[a-zA-ZÀ-Ỵà-ỹ]', part) else ' ' for part in header_parts)
            return new_header
        return header

    # Tìm các block Markdown table
    return re.sub(r'((?:\|\s*.*\s*)\|(?:\s*\n)+\|(?:[-:| ]+)\|)', remove_fake_column_titles, md)

# 🔍 Quét thư mục hiện tại, lấy tất cả file .pdf
current_dir = os.getcwd()
pdf_files = [f for f in os.listdir(current_dir) if f.lower().endswith(".pdf")]

if not pdf_files:
    print("Không tìm thấy file PDF nào trong thư mục.")
else:
    for pdf_file in sorted(pdf_files):
        pdf_path = os.path.join(current_dir, pdf_file)
        md_name = pdf_file.replace('.pdf', '.md')

        try:
            print(f"Đang xử lý: {pdf_file} ...")
            raw_md = to_markdown(pdf_path)
            cleaned_md = clean_md(raw_md)
            final_md = fix_table_column_headers(cleaned_md)

            with open(os.path.join(current_dir, md_name), "w", encoding="utf-8") as f:
                f.write(final_md)
            print(f"✅ Ghi xong: {md_name}")
        except Exception as e:
            print(f"❌ Lỗi với {pdf_file}: {e}")