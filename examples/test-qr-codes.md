# QRieux Test QR Codes

Scan these QR codes to test all content types supported by the app.

## URLs

| QR | Content | Label |
|:--:|---------|-------|
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=https://example.com) | `https://example.com` | Basic HTTPS |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=http://test.org/path?q=1%26x=2) | `http://test.org/path?q=1&x=2` | HTTP + path + query params |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=https://sub.domain.co.uk/a/b/c) | `https://sub.domain.co.uk/a/b/c` | Subdomain + multi-level TLD |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=https://github.com/user/repo%23readme) | `https://github.com/user/repo#readme` | URL with fragment |

## Emails

| QR | Content | Label |
|:--:|---------|-------|
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=mailto:test@example.com) | `mailto:test@example.com` | mailto: prefix |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=user@domain.org) | `user@domain.org` | Direct email (no prefix) |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=name.surname%2Btag@mail.co.uk) | `name.surname+tag@mail.co.uk` | Complex email (dots, plus, subdomain) |

## Phone Numbers

| QR | Content | Label |
|:--:|---------|-------|
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=tel:%2B1234567890) | `tel:+1234567890` | tel: prefix |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=%2B33%206%2012%2034%2056%2078) | `+33 6 12 34 56 78` | Spaced international (France) |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=(555)%20123-4567) | `(555) 123-4567` | US format with parentheses |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=00491234567890) | `00491234567890` | International (no + prefix) |

## Plain Text

| QR | Content | Label |
|:--:|---------|-------|
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=Hello%20World) | `Hello World` | Simple text |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=WiFi:%20MyNetwork%0APass:%20secret123) | `WiFi: MyNetwork` / `Pass: secret123` | Multi-line info |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=Meeting%20Room%203B%20-%202pm%20Tomorrow) | `Meeting Room 3B - 2pm Tomorrow` | Mixed alphanumeric |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=%E4%BD%A0%E5%A5%BD%E4%B8%96%E7%95%8C) | `你好世界` | Unicode / CJK |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=ORDER-2024-ABC123) | `ORDER-2024-ABC123` | Order/ID format |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=Lorem%20ipsum%20dolor%20sit%20amet%20consectetur%20adipiscing%20elit) | `Lorem ipsum dolor sit amet...` | Longer text |

## Edge Cases

| QR | Content | Label |
|:--:|---------|-------|
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=ftp://files.example.com) | `ftp://files.example.com` | Non-http URL (should be Text) |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=www.example.com) | `www.example.com` | No protocol (should be Text) |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=12345) | `12345` | Short number (not phone) |
| ![QR](https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=%20%20spaced%20text%20%20) | `  spaced text  ` | Leading/trailing spaces |
