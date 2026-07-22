#!/usr/bin/env node
const fs = require('fs');
const path = require('path');

const DOC_PATH = path.join(__dirname, '../test-mobile-api.md');
const TESTS_DIR = path.join(__dirname, '../tests');

// 1. Phân tích tài liệu đặc tả markdown
function parseSpecification() {
  if (!fs.existsSync(DOC_PATH)) {
    console.error(`Không tìm thấy file đặc tả tại: ${DOC_PATH}`);
    process.exit(1);
  }

  const content = fs.readFileSync(DOC_PATH, 'utf8');
  const lines = content.split('\n');

  const activeCases = new Map(); // id -> line description
  const deletedCases = new Map(); // id -> line description

  // Regex để quét: tìm [DELETED] (nếu có) và TC_XXX
  const tcRegex = /(?:\[DELETED\]\s+)?TC_[A-Z0-9_]+/g;

  lines.forEach((line, index) => {
    const matches = line.match(tcRegex);
    if (matches) {
      matches.forEach(match => {
        const isDeleted = match.startsWith('[DELETED]');
        const tcId = isDeleted 
          ? match.replace('[DELETED]', '').trim() 
          : match;
        
        // Lấy mô tả ngắn (bỏ các ký tự định dạng markdown ở đầu và dấu ** ở cuối)
        const cleanLine = line.trim()
          .replace(/^[\s*\-#\d.]+\s*/, '')
          .replace(/\*\*$/, '')
          .trim();

        if (isDeleted) {
          deletedCases.set(tcId, { line: index + 1, desc: cleanLine });
        } else {
          activeCases.set(tcId, { line: index + 1, desc: cleanLine });
        }
      });
    }
  });

  return { activeCases, deletedCases };
}

// 2. Phân tích các file test thực tế trong tests/
function parseImplementations() {
  if (!fs.existsSync(TESTS_DIR)) {
    console.error(`Không tìm thấy thư mục tests tại: ${TESTS_DIR}`);
    process.exit(1);
  }

  const implementedCases = new Map(); // id -> file location path
  const files = fs.readdirSync(TESTS_DIR);

  files.forEach(file => {
    if (file.endsWith('.test.ts') || file.endsWith('.test.js')) {
      const filePath = path.join(TESTS_DIR, file);
      const content = fs.readFileSync(filePath, 'utf8');
      
      // Quét tất cả các từ dạng TC_XXX
      const tcRegex = /TC_[A-Z0-9_]+/g;
      const matches = content.match(tcRegex);
      if (matches) {
        matches.forEach(tcId => {
          if (!implementedCases.has(tcId)) {
            implementedCases.set(tcId, []);
          }
          implementedCases.get(tcId).push(file);
        });
      }
    }
  });

  return implementedCases;
}

function main() {
  console.log('\n======================================================');
  console.log('  HỆ THỐNG ĐỐI CHIẾU VÀ TỰ ĐỘNG KIỂM TRA TESTCASES  ');
  console.log('======================================================\n');

  const { activeCases, deletedCases } = parseSpecification();
  const implementedCases = parseImplementations();

  const missingCases = [];
  const redundantCases = [];
  const correctCases = [];

  // Tìm các testcase thiếu (có trong spec hoạt động nhưng chưa implement)
  activeCases.forEach((info, id) => {
    if (!implementedCases.has(id)) {
      missingCases.push({ id, ...info });
    } else {
      correctCases.push({ id, ...info, files: implementedCases.get(id) });
    }
  });

  // Tìm các testcase cần xóa (đã implement nhưng bị DELETED hoặc không có trong spec)
  implementedCases.forEach((files, id) => {
    if (deletedCases.has(id)) {
      redundantCases.push({ id, reason: `Được đánh dấu [DELETED] ở dòng ${deletedCases.get(id).line} của test-mobile-api.md`, files });
    } else if (!activeCases.has(id)) {
      redundantCases.push({ id, reason: 'Không tồn tại trong tài liệu test-mobile-api.md', files });
    }
  });

  // Xuất báo cáo
  console.log(`Tổng số testcase hoạt động trong tài liệu: ${activeCases.size}`);
  console.log(`Tổng số testcase bị hủy bỏ [DELETED]: ${deletedCases.size}`);
  console.log(`Tổng số testcase đã implement trong code: ${implementedCases.size}`);
  console.log(`Đã implement khớp chuẩn: ${correctCases.length}\n`);

  console.log('------------------------------------------------------');
  if (missingCases.length > 0) {
    console.log(`\x1b[33m[THIẾU] DANH SÁCH ${missingCases.length} TESTCASE CẦN THÊM MỚI (CHƯA IMPLEMENT):\x1b[0m`);
    missingCases.forEach(item => {
      console.log(`  - \x1b[33m${item.id}\x1b[0m (Dòng ${item.line} trong đặc tả): ${item.desc}`);
    });
  } else {
    console.log('\x1b[32m[OK] Không có testcase nào bị thiếu. Đã implement đầy đủ!\x1b[0m');
  }

  console.log('------------------------------------------------------');
  if (redundantCases.length > 0) {
    console.log(`\x1b[31m[DƯ THỪA / CẦN XÓA] DANH SÁCH ${redundantCases.length} TESTCASE CẦN LOẠI BỎ TRONG CODE:\x1b[0m`);
    redundantCases.forEach(item => {
      console.log(`  - \x1b[31m${item.id}\x1b[0m (Có trong: ${item.files.join(', ')}): ${item.reason}`);
    });
  } else {
    console.log('\x1b[32m[OK] Không có testcase dư thừa hoặc đã bị hủy bỏ trong code test!\x1b[0m');
  }
  console.log('======================================================\n');
}

main();
