import QRCode from 'qrcode';
import path from 'path';

const fixtures: Record<string, string> = {
  'url-https.png': 'https://example.com',
  'email-mailto.png': 'mailto:test@example.com',
  'phone-tel.png': 'tel:+1234567890',
  'text-hello.png': 'Hello World',
  'wifi.png': 'WIFI:T:WPA;S:TestNetwork;P:password123;;',
};

const outDir = path.join(__dirname, '..', 'fixtures');

async function main() {
  for (const [filename, content] of Object.entries(fixtures)) {
    const filepath = path.join(outDir, filename);
    await QRCode.toFile(filepath, content, {
      width: 800,
      margin: 4,
      errorCorrectionLevel: 'H',
    });
    console.log(`Generated ${filename} → "${content}"`);
  }
  console.log(`\nAll fixtures written to ${outDir}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
