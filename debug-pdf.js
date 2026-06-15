const { getDocument } = require("pdfjs-dist/legacy/build/pdf");
const fs = require("fs");

const pdfPath = "ClasificacionMundial_2026.pdf";
const searchName = "JaimeDeMariaSanchez";

async function main() {
  const data = new Uint8Array(fs.readFileSync(pdfPath).buffer);
  const doc = await getDocument({ data }).promise;

  // Find the page where the name appears in raw text
  for (let pg = 1; pg <= doc.numPages; pg++) {
    const page = await doc.getPage(pg);
    const tc = await page.getTextContent();
    // Get items in reading order (sorted by Y then X)
    const items = tc.items.map(it => ({ str: it.str, x: it.transform[4], y: it.transform[5] }))
      .sort((a, b) => a.y !== b.y ? a.y - b.y : a.x - b.x);
    
    // Build full text (without spaces)
    let fullNoSpaces = '';
    const charList = [];
    for (const item of items) {
      for (const ch of item.str) {
        if (ch.trim()) {
          charList.push({ text: ch, x: item.x, y: item.y });
          fullNoSpaces += ch;
        }
      }
    }

    // Search for the name in raw text (normalized)
    const normFull = fullNoSpaces.toLowerCase()
      .replace(/í/g, "i").replace(/é/g, "e").replace(/á/g, "a")
      .replace(/ó/g, "o").replace(/ú/g, "u").replace(/ñ/g, "n");
    const normName = searchName.toLowerCase()
      .replace(/í/g, "i").replace(/é/g, "e").replace(/á/g, "a")
      .replace(/ó/g, "o").replace(/ú/g, "u").replace(/ñ/g, "n");

    const idx = normFull.indexOf(normName);
    if (idx >= 0) {
      console.log(`\n========== PAGE ${pg} ==========`);
      console.log(`Found '${searchName}' at char offset ${idx}`);
      // Get the X,Y of the first character
      const firstChar = charList[idx];
      console.log(`First char '${firstChar.text}' at x=${firstChar.x.toFixed(1)} y=${firstChar.y.toFixed(1)}`);
      
      // Context
      const ctxStart = Math.max(0, idx - 20);
      const ctxEnd = Math.min(fullNoSpaces.length, idx + normName.length + 20);
      console.log(`Context: ...${fullNoSpaces.substring(ctxStart, ctxEnd)}...`);
      
      // Now get per-page structure (same as before)
      // Group by Y and find position row
      const yGroups = {};
      for (const item of items) {
        for (const ch of item.str) {
          if (ch.trim()) {
            const yKey = Math.round(item.y);
            if (!yGroups[yKey]) yGroups[yKey] = [];
            yGroups[yKey].push({ x: item.x, y: item.y, text: ch });
          }
        }
      }

      // Find digit rows
      const candidateYs = Object.keys(yGroups).map(Number).sort((a, b) => a - b);
      for (const yKey of candidateYs) {
        const grp = yGroups[yKey];
        if (grp.length < 5) continue;
        const allDigits = grp.every(it => it.text.trim().split("").every(c => /\d/.test(c)) || it.text.trim() === "");
        if (!allDigits) continue;
        // Show it
        const digitItems = grp.filter(it => it.text.trim().length > 0 && /\d/.test(it.text.trim()))
          .sort((a, b) => a.x - b.x);
        if (digitItems.length < 5) continue;
        // Group by proximity
        const groups = [];
        let cur = "", curX = 0;
        for (const d of digitItems) {
          const digit = d.text.trim();
          if (cur === "") { cur = digit; curX = d.x; }
          else if (d.x - curX < 3) { cur += digit; }
          else {
            const n = parseInt(cur, 10);
            if (!isNaN(n)) groups.push(n);
            cur = digit; curX = d.x;
          }
        }
        if (cur !== "") {
          const n = parseInt(cur, 10);
          if (!isNaN(n)) groups.push(n);
        }
        if (groups.length >= 5) {
          const sorted = [...groups].sort((a, b) => a - b);
          // Show first 5 and last 3
          const showFirst = groups.slice(0, 5).join(",");
          const showLast = groups.slice(-3).join(",");
          let seq = true;
          for (let i = 1; i < sorted.length; i++) {
            if (sorted[i] !== sorted[i-1] + 1) { seq = false; break; }
          }
          console.log(`  Y=${yKey} groups=${groups.length} seq=${seq}: [${showFirst}...${showLast}]`);
        }
      }
    }
  }

  doc.destroy();
}

main().catch(console.error);
