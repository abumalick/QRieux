#!/usr/bin/env python3
"""Fail if an APK carries a signing block F-Droid refuses.

F-Droid's scanner rejects anything in the APK Signing Block beyond signatures
and padding. The Android Gradle Plugin adds a dependency-metadata block by
default, which silently breaks the reproducible-build check long after the
release is published, so this runs before the APK is uploaded.
"""
import struct
import sys

ALLOWED = {
    0x7109871A: "APK signature scheme v2",
    0xF05368C0: "APK signature scheme v3",
    0x1B93AD61: "verity padding",
    0x42726577: "padding",
}
KNOWN_BAD = {
    0x504B4453: "dependency metadata (set dependenciesInfo.includeInApk = false)",
    0x6DFF800D: "source stamp",
}


def signing_blocks(path):
    data = open(path, "rb").read()
    eocd = data.rfind(b"PK\x05\x06")
    if eocd < 0:
        raise ValueError("not a zip/apk")
    central_dir = struct.unpack("<I", data[eocd + 16 : eocd + 20])[0]
    if data[central_dir - 16 : central_dir] != b"APK Sig Block 42":
        return []
    size = struct.unpack("<Q", data[central_dir - 24 : central_dir - 16])[0]
    pos = central_dir - size - 8 + 8
    end = central_dir - 24
    while pos < end:
        length = struct.unpack("<Q", data[pos : pos + 8])[0]
        yield struct.unpack("<I", data[pos + 8 : pos + 12])[0], length
        pos += 8 + length


def main(paths):
    failed = False
    for path in paths:
        print(f"{path}:")
        for block_id, length in signing_blocks(path):
            name = ALLOWED.get(block_id) or KNOWN_BAD.get(block_id) or "unrecognised block"
            mark = "ok  " if block_id in ALLOWED else "FAIL"
            print(f"  {mark} 0x{block_id:08x} {length:>8} bytes  {name}")
            if block_id not in ALLOWED:
                failed = True
    if failed:
        print("\nF-Droid will reject this APK. Rebuild without the offending block.")
    return 1 if failed else 0


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit("usage: check_apk_signing_blocks.py <apk>...")
    sys.exit(main(sys.argv[1:]))
