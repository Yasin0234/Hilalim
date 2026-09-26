const fs = require('fs');

const orig = JSON.parse(fs.readFileSync('D:/Hilalim/app/src/main/assets/quran_verses.json', 'utf8'));
const updated = JSON.parse(fs.readFileSync('D:/Hilalim/temp_output.json', 'utf8'));

console.log('Total surahs in orig:', orig.length, 'Total surahs in updated:', updated.length);

if (orig.length !== updated.length) {
  console.error('FAIL: Surah count mismatch!');
  process.exit(1);
}

// Check other surahs
for (let s of updated) {
  const origS = orig.find(x => x.surah_id === s.surah_id);
  if (!origS) {
    console.error('FAIL: Missing surah ' + s.surah_id);
    process.exit(1);
  }

  // Ensure sorted by ID ascending
  for (let i = 0; i < s.verses.length - 1; i++) {
    if (s.verses[i].id >= s.verses[i + 1].id) {
      console.error('FAIL: Verses not sorted ascending in surah ' + s.surah_id + ': ' + s.verses[i].id + ' >= ' + s.verses[i+1].id);
      process.exit(1);
    }
  }

  // Check required structure
  for (let v of s.verses) {
    if (typeof v.id !== 'number' || typeof v.ar !== 'string' || typeof v.tr !== 'string') {
      console.error('FAIL: Invalid verse format in surah ' + s.surah_id + ', verse ' + JSON.stringify(v));
      process.exit(1);
    }
    if (!v.ar || !v.tr) {
      console.error('FAIL: Empty ar or tr in surah ' + s.surah_id + ', verse id ' + v.id);
      process.exit(1);
    }
  }

  if (![2, 3, 4, 5].includes(s.surah_id)) {
    // Other surahs must be identical
    if (JSON.stringify(s) !== JSON.stringify(origS)) {
      console.error('FAIL: Surah ' + s.surah_id + ' changed unexpectedly!');
      process.exit(1);
    }
  }
}

// Verify Surah 2
const s2 = updated.find(s => s.surah_id === 2);
const s2Ids = s2.verses.map(v => v.id);
const expectedS2 = [];
for (let i = 1; i <= 200; i++) expectedS2.push(i);
expectedS2.push(255, 285, 286);
if (JSON.stringify(s2Ids) !== JSON.stringify(expectedS2)) {
  console.error('FAIL: Surah 2 IDs mismatch!');
  console.error('Actual:', s2Ids);
  process.exit(1);
}

// Verify Surah 3
const s3 = updated.find(s => s.surah_id === 3);
const s3Ids = s3.verses.map(v => v.id);
const expectedS3 = [];
for (let i = 1; i <= 120; i++) expectedS3.push(i);
if (JSON.stringify(s3Ids) !== JSON.stringify(expectedS3)) {
  console.error('FAIL: Surah 3 IDs mismatch!');
  process.exit(1);
}

// Verify Surah 4
const s4 = updated.find(s => s.surah_id === 4);
const s4Ids = s4.verses.map(v => v.id);
const expectedS4 = [];
for (let i = 1; i <= 120; i++) expectedS4.push(i);
if (JSON.stringify(s4Ids) !== JSON.stringify(expectedS4)) {
  console.error('FAIL: Surah 4 IDs mismatch!');
  process.exit(1);
}

// Verify Surah 5
const s5 = updated.find(s => s.surah_id === 5);
const s5Ids = s5.verses.map(v => v.id);
const expectedS5 = [];
for (let i = 1; i <= 80; i++) expectedS5.push(i);
if (JSON.stringify(s5Ids) !== JSON.stringify(expectedS5)) {
  console.error('FAIL: Surah 5 IDs mismatch!');
  process.exit(1);
}

console.log('ALL VERIFICATION CHECKS PASSED SUCCESSFULLY!');
