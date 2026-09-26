const fs = require('fs');

async function processQuranData() {
  const filePath = 'D:/Hilalim/app/src/main/assets/quran_verses.json';
  const data = JSON.parse(fs.readFileSync(filePath, 'utf8'));

  function cleanTr(text) {
    return text
      .replace(/&quot;/g, "'")
      .replace(/&#39;/g, "'")
      .replace(/&apos;/g, "'")
      .replace(/&amp;/g, '&')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .trim();
  }

  const tasks = [
    { chapter: 2, min: 151, max: 200 },
    { chapter: 3, min: 91, max: 120 },
    { chapter: 4, min: 91, max: 120 },
    { chapter: 5, min: 61, max: 80 }
  ];

  for (let t of tasks) {
    const url = 'https://api.quran.com/api/v4/verses/by_chapter/' + t.chapter + '?language=tr&translations=77&fields=text_imlaei&page=1&per_page=300';
    const res = await fetch(url);
    const json = await res.json();

    const newVerses = json.verses
      .filter(v => v.verse_number >= t.min && v.verse_number <= t.max)
      .map(v => ({
        id: v.verse_number,
        ar: v.text_imlaei,
        tr: cleanTr(v.translations[0] ? v.translations[0].text : '')
      }));

    console.log('Fetched Surah ' + t.chapter + ': ' + newVerses.length + ' new verses (IDs ' + newVerses[0].id + ' to ' + newVerses[newVerses.length - 1].id + ')');

    const surahObj = data.find(s => s.surah_id === t.chapter);
    if (!surahObj) {
      console.error('Surah ' + t.chapter + ' not found in JSON!');
      process.exit(1);
    }

    const verseMap = new Map();
    surahObj.verses.forEach(v => verseMap.set(v.id, v));
    newVerses.forEach(v => verseMap.set(v.id, v));

    const sortedVerses = Array.from(verseMap.values()).sort((a, b) => a.id - b.id);
    surahObj.verses = sortedVerses;

    console.log('Updated Surah ' + t.chapter + ': total verses = ' + surahObj.verses.length + ', min ID = ' + surahObj.verses[0].id + ', max ID = ' + surahObj.verses[surahObj.verses.length - 1].id);
  }

  // Save result to temp output file
  const resultJson = JSON.stringify(data, null, 2);
  fs.writeFileSync('D:/Hilalim/temp_output.json', resultJson, 'utf8');
  console.log('Output written to D:/Hilalim/temp_output.json successfully. Size: ' + resultJson.length + ' bytes');
}

processQuranData().catch(err => {
  console.error(err);
  process.exit(1);
});
