import { getDocument } from "pdfjs-dist";
import fs from "fs";

const pdfPath = "ClasificacionMundial_2026.pdf";
const searchName = "JaimeDeMariaSanchez";

// Normalize
function normalize(s) {
  return s
    .toLowerCase()
    .replace(/í/g, "i")
    .replace(/é/g, "e")
    .replace(/á/g, "a")
    .replace(/ó/g, "o")
    .replace(/ú/g, "u")
    .replace(/ñ/g, "n");
}

async function main() {
  const data = new Uint8Array(fs.readFileSync(pdfPath).buffer);
  const doc = await getDocument({ data }).promise;

  const allItems = []; // { x, y, text, pg }

  for (let pg = 1; pg <= doc.numPages; pg++) {
    const page = await doc.getPage(pg);
    const textContent = await page.getTextContent();
    for (const item of textContent.items) {
      const ch = item.str;
      if (ch && ch.trim()) {
        allItems.push({
          x: item.transform[4],
          y: item.transform[5],
          text: ch,
          pg,
        });
      }
    }
  }

  console.log("Total items:", allItems.length);
  console.log("First items:", allItems.slice(0, 5));

  // Find the name
  const searchNorm = normalize(searchName);
  let targetPage = -1;
  let targetX = -1;

  for (let i = 0; i <= allItems.length - searchNorm.length; i++) {
    if (normalize(allItems[i].text) === searchNorm[0]) {
      let match = true;
      for (let j = 1; j < searchNorm.length; j++) {
        if (
          i + j >= allItems.length ||
          normalize(allItems[i + j].text) !== searchNorm[j]
        ) {
          match = false;
          break;
        }
      }
      if (match) {
        targetPage = allItems[i].pg;
        targetX = allItems[i].x;
        console.log(
          `\nName found at pg=${targetPage}, x=${targetX}, y=${allItems[i].y}`
        );
        break;
      }
    }
  }

  if (targetPage < 0) {
    console.log("Name not found");
    return;
  }

  // Group by page and Y
  const pageYGroups = {};
  for (const item of allItems) {
    if (!pageYGroups[item.pg]) pageYGroups[item.pg] = {};
    const yKey = Math.round(item.y);
    if (!pageYGroups[item.pg][yKey]) pageYGroups[item.pg][yKey] = [];
    pageYGroups[item.pg][yKey].push(item);
  }

  // --- EMULATE findPositionRow ---
  function findPositionRow(yg) {
    const allPositions = [];
    const candidateYs = Object.keys(yg)
      .map(Number)
      .sort((a, b) => a - b);

    for (const yKey of candidateYs) {
      const items = yg[yKey];
      if (items.length < 5) continue;

      // Check all items are digits or blank
      const allDigitsOrBlank = items.every(
        (it) =>
          it.text.trim().split("").every((c) => /\d/.test(c)) ||
          it.text.trim() === ""
      );
      if (!allDigitsOrBlank) {
        continue;
      }

      // console.log(`  Y=${yKey} digits/blank check passed, items=${items.length}`);

      const digitItems = items
        .filter(
          (it) =>
            it.text.trim().length > 0 &&
            it.text.trim().split("").every((c) => /\d/.test(c))
        )
        .sort((a, b) => a.x - b.x);

      if (digitItems.length < 5) continue;

      const groups = [];
      let current = "";
      let currentX = 0;

      for (const d of digitItems) {
        const digit = d.text.trim();
        if (current === "") {
          current = digit;
          currentX = d.x;
        } else if (d.x - currentX < 3) {
          current += digit;
        } else {
          const num = parseInt(current, 10);
          if (!isNaN(num)) groups.push({ pos: num, x: currentX });
          current = digit;
          currentX = d.x;
        }
      }
      if (current !== "") {
        const num = parseInt(current, 10);
        if (!isNaN(num)) groups.push({ pos: num, x: currentX });
      }

      if (groups.length < 5) continue;

      // Check sequential
      const sorted = groups.map((g) => g.pos).sort((a, b) => a - b);
      let sequential = true;
      for (let i = 1; i < sorted.length; i++) {
        if (sorted[i] !== sorted[i - 1] + 1) {
          sequential = false;
          break;
        }
      }
      if (sequential) {
        console.log(
          `  >> Row Y=${yKey}: ${groups.length} positions from ${sorted[0]} to ${sorted[sorted.length - 1]}`
        );
        allPositions.push(...groups);
      }
    }
    allPositions.sort((a, b) => a.pos - b.pos);
    return allPositions.length > 0 ? allPositions : null;
  }

  // Count positions on pages before target page
  let position = 0;
  for (let pg = 1; pg < targetPage; pg++) {
    const yg = pageYGroups[pg];
    if (!yg) continue;
    console.log(`\n--- Page ${pg} ---`);
    const posData = findPositionRow(yg);
    if (!posData) {
      console.log(`  No position row found`);
      continue;
    }
    console.log(`  Found ${posData.length} positions`);
    position += posData.length;
  }

  console.log(`\n--- Target Page ${targetPage} (name at x=${targetX}) ---`);
  const targetYg = pageYGroups[targetPage];
  const targetPositions = findPositionRow(targetYg);
  if (!targetPositions) {
    console.log("No target positions found");
    return;
  }

  console.log(`  Target positions: ${targetPositions.length} items`);
  console.log(`  First: ${targetPositions[0].pos}, Last: ${targetPositions[targetPositions.length - 1].pos}`);

  let bestPos = targetPositions[0].pos;
  let bestDist = Infinity;
  for (const { pos, x } of targetPositions) {
    const dist = Math.abs(x - targetX);
    if (dist < bestDist) {
      bestDist = dist;
      bestPos = pos;
    }
  }
  console.log(`  Best pos: ${bestPos} (dist=${bestDist})`);

  position += bestPos - targetPositions[0].pos + 1;
  console.log(`\n*** Final position: ${position} ***`);
}

main().catch(console.error);
