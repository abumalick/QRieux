import QRCode from 'qrcode';
import { writeFileSync } from 'fs';
import path from 'path';

const fixtures: Record<string, string> = {
  'url-https.png': 'https://example.com',
  'email-mailto.png': 'mailto:test@example.com',
  'phone-tel.png': 'tel:+1234567890',
  'text-hello.png': 'Hello World',
  'wifi.png': 'WIFI:T:WPA;S:TestNetwork;P:password123;;',
  'vcard.png': 'BEGIN:VCARD\nVERSION:3.0\nFN:John Doe\nTEL:+1234567890\nEMAIL:john@example.com\nORG:ACME Corp\nEND:VCARD',
};

const outDir = path.join(__dirname, '..', 'fixtures');

// Minimal valid 1x1 white PNG (no external deps)
function generateBlankPng(filepath: string): void {
  const png = Buffer.from(
    '89504e470d0a1a0a0000000d4948445200000001000000010802000000907753de0000000c4944415408d76360f8cf00000001010000182dd5e30000000049454e44ae426082',
    'hex',
  );
  writeFileSync(filepath, png);
}

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

  const noQrPath = path.join(outDir, 'no-qr.png');
  generateBlankPng(noQrPath);
  console.log('Generated no-qr.png → (blank image, no QR code)');

  console.log(`\nAll fixtures written to ${outDir}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
