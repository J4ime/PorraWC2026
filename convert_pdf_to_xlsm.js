const fs = require('fs');
const pdfjsLib = require('pdfjs-dist');
const XLSX = require('xlsx');

async function extractPageData(page) {
    const tc = await page.getTextContent();
    return tc.items.map(item => ({
        x: Math.round(item.transform[4]),
        y: Math.round(item.transform[5]),
        text: item.str
    }));
}

function groupByY(items) {
    const rows = {};
    for (const item of items) {
        const key = item.y;
        if (!rows[key]) rows[key] = [];
        rows[key].push(item);
    }
    return rows;
}

function isNumeric(s) { return /^\d+$/.test(s.trim()); }

function parsePage(items, startPos) {
    const rows = groupByY(items);
    const sortedYs = Object.keys(rows).map(Number).sort((a, b) => b - a);

    // Find position row
    let posRow = null, posY = 0;
    for (const y of sortedYs) {
        const row = rows[y].sort((a, b) => a.x - b.x);
        const nums = row.filter(m => isNumeric(m.text.trim()) && m.text.trim().length <= 3);
        if (nums.length >= 30) { posRow = row; posY = y; break; }
    }
    if (!posRow) return [];

    const posColumns = posRow.filter(m => isNumeric(m.text.trim()))
        .map(m => ({ pos: parseInt(m.text.trim(), 10), x: m.x }));
    const colWidth = posColumns.length > 1 ? posColumns[1].x - posColumns[0].x : 7;

    // Collect name items from multiple adjacent y-levels below the position row
    let allNameItems = [];
    let foundNameY = 0;
    for (const y of sortedYs) {
        if (y >= posY) continue;
        const row = rows[y].sort((a, b) => a.x - b.x);
        const alpha = row.filter(m => {
            const t = m.text.trim();
            return t !== '|' && t.length > 0 && /[a-zA-ZÀ-ÿ0-9_ -]/.test(t);
        });
        if (alpha.length >= Math.min(posColumns.length, 20)) {
            allNameItems = allNameItems.concat(alpha);
            if (foundNameY === 0) foundNameY = y;
        } else if (foundNameY > 0 && y < foundNameY - 5) {
            // Stop after passing the main name row
            break;
        }
    }

    if (allNameItems.length === 0) return [];

    // Merge name items that are close in x-position (within 4px)
    const sortedItems = [...allNameItems].sort((a, b) => a.x - b.x);
    const merged = [];
    let current = null;
    for (const item of sortedItems) {
        const t = item.text.trim();
        if (t.length === 0) continue;
        if (current === null) {
            current = { x: item.x, text: t };
        } else if (item.x - current.x <= 5) {
            current.text += t;
        } else {
            merged.push(current);
            current = { x: item.x, text: t };
        }
    }
    if (current) merged.push(current);

    // Now map merged names to position columns
    const posNames = {};
    for (const pc of posColumns) posNames[pc.pos] = [];

    for (const item of merged) {
        let bestPos = null, bestDist = Infinity;
        for (const pc of posColumns) {
            const dist = Math.abs(item.x - pc.x);
            if (dist < bestDist) { bestDist = dist; bestPos = pc.pos; }
        }
        if (bestPos !== null && bestDist <= colWidth) {
            posNames[bestPos].push(item.text);
        }
    }

    // Find points and jornada rows (using same multi-row approach)
    let ptsItems = [];
    for (const y of sortedYs) {
        if (y >= foundNameY) continue;
        const row = rows[y].sort((a, b) => a.x - b.x);
        const nums = row.filter(m => {
            const t = m.text.trim();
            return t !== '|' && t.length > 0 && isNumeric(t) && parseInt(t, 10) < 1000;
        });
        if (nums.length >= Math.min(posColumns.length, 15)) {
            ptsItems = ptsItems.concat(nums);
            break;
        }
    }

    const ptsMap = {};
    for (const m of ptsItems) {
        const t = m.text.trim();
        if (t === '|' || t.length === 0 || !isNumeric(t)) continue;
        const v = parseInt(t, 10);
        if (v >= 1000) continue;
        let bestPos = null, bestDist = Infinity;
        for (const pc of posColumns) {
            const dist = Math.abs(m.x - pc.x);
            if (dist < bestDist) { bestDist = dist; bestPos = pc.pos; }
        }
        if (bestPos !== null && bestDist <= colWidth) ptsMap[bestPos] = v;
    }

    let jornItems = [];
    const ptsY = ptsItems.length > 0 ? ptsItems[0].y : foundNameY;
    for (const y of sortedYs) {
        if (y >= ptsY) continue;
        const row = rows[y].sort((a, b) => a.x - b.x);
        const nums = row.filter(m => {
            const t = m.text.trim();
            return t !== '|' && t.length > 0 && isNumeric(t);
        });
        if (nums.length >= Math.min(posColumns.length, 5)) {
            jornItems = jornItems.concat(nums);
            break;
        }
    }

    const jornMap = {};
    for (const m of jornItems) {
        const t = m.text.trim();
        if (t === '|' || t.length === 0 || !isNumeric(t)) continue;
        const v = parseInt(t, 10);
        let bestPos = null, bestDist = Infinity;
        for (const pc of posColumns) {
            const dist = Math.abs(m.x - pc.x);
            if (dist < bestDist) { bestDist = dist; bestPos = pc.pos; }
        }
        if (bestPos !== null && bestDist <= colWidth) jornMap[bestPos] = v;
    }

    const result = [];
    for (const pc of posColumns) {
        const fragments = posNames[pc.pos] || [];
        result.push({
            pos: pc.pos,
            name: fragments.join(''),
            points: ptsMap[pc.pos] || 0,
            jornada: jornMap[pc.pos] || 0
        });
    }
    return result;
}

async function main() {
    const bin = fs.readFileSync('ClasificacionMundial_2026.pdf');
    const data = new Uint8Array(bin);
    const doc = await pdfjsLib.getDocument({ data }).promise;

    let allData = [];
    for (let p = 1; p <= doc.numPages; p++) {
        const page = await doc.getPage(p);
        const items = await extractPageData(page);
        const pageData = parsePage(items, 0);
        console.log(`Page ${p}: ${pageData.length} entries`);
        if (pageData.length > 0) {
            const short = pageData.filter(d => d.name.length <= 2);
            if (short.length > 0) {
                console.log(`  Fragments: ${short.length}`);
                short.forEach(s => console.log(`    Pos ${s.pos}: "${s.name}" pts=${s.points}`));
            }
            allData = allData.concat(pageData);
        }
    }

    allData.sort((a, b) => a.pos - b.pos);
    console.log(`\nTotal: ${allData.length} entries`);

    // Verify data around known trouble spots
    const short = allData.filter(e => e.name.length <= 2);
    if (short.length > 0) {
        console.log('\nRemaining fragments:');
        for (const s of short) {
            // Show neighbors
            const prev = allData.find(e => e.pos === s.pos - 1);
            const next = allData.find(e => e.pos === s.pos + 1);
            console.log(`  Pos ${s.pos}: "${s.name}" pts=${s.points}`);
            if (prev) console.log(`    Previous: Pos ${prev.pos} "${prev.name}" pts=${prev.points}`);
            if (next) console.log(`    Next: Pos ${next.pos} "${next.name}" pts=${next.points}`);
        }
    } else {
        console.log('No fragment names detected');
    }

    // Create workbook
    const wb = XLSX.utils.book_new();
    const wsData = [['Posición', 'Nombre', 'Puntos Totales', 'Puntos Jornada']];
    for (const e of allData) wsData.push([e.pos, e.name, e.points, e.jornada]);
    const ws = XLSX.utils.aoa_to_sheet(wsData);
    XLSX.utils.book_append_sheet(wb, ws, 'Clasificacion');
    ws['!cols'] = [{ wch: 8 }, { wch: 35 }, { wch: 15 }, { wch: 15 }];

    const outPath = 'ClasificacionMundial_2026.xlsm';
    XLSX.writeFile(wb, outPath);
    console.log(`\nCreated: ${outPath}`);
}

main().catch(err => console.error('Error:', err));
