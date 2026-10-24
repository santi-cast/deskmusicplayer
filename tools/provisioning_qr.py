"""Builds the QR code that makes DeskMusicPlayer device owner from Android's setup wizard.

The QR holds Android's provisioning extras: the admin component, where to download the APK
(always the latest release, under a fixed file name) and the SHA-256 of the signing certificate,
so Android only accepts an APK signed with the project key. The same QR is valid for every
version signed with that key.

Usage:
    python3 provisioning_qr.py <signing certificate SHA-256 or signed APK> <output directory>

With an APK, the certificate digest is read with apksigner from the Android SDK
(ANDROID_HOME or ~/Android/Sdk). The PNG is written only if the `segno` package is available.
"""

import base64
import glob
import json
import os
import re
import subprocess
import sys

COMPONENT = "com.reproductor.kiosk/com.reproductor.kiosk.AdminReceiver"
DOWNLOAD = "https://github.com/santi-cast/deskmusicplayer/releases/latest/download/deskmusicplayer.apk"


def checksum(cert_sha256: str) -> str:
    """Certificate SHA-256 (hex, colons allowed) as URL-safe base64 without padding."""
    hex_digest = cert_sha256.replace(":", "").strip().lower()
    if not re.fullmatch(r"[0-9a-f]{64}", hex_digest):
        raise ValueError("expected a SHA-256 digest of 64 hex characters")
    return base64.urlsafe_b64encode(bytes.fromhex(hex_digest)).decode().rstrip("=")


def payload(cert_sha256: str) -> str:
    return json.dumps({
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": COMPONENT,
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": DOWNLOAD,
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": checksum(cert_sha256),
        "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": True,
    }, separators=(",", ":"))


def cert_digest_from_apk(apk: str) -> str:
    sdk = os.environ.get("ANDROID_HOME") or os.path.expanduser("~/Android/Sdk")
    tools = sorted(glob.glob(os.path.join(sdk, "build-tools", "*", "apksigner")))
    if not tools:
        raise SystemExit("apksigner not found in " + sdk)
    out = subprocess.run([tools[-1], "verify", "--print-certs", apk], capture_output=True, text=True, check=True).stdout
    match = re.search(r"Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})", out)
    if not match:
        raise SystemExit("could not read the signing certificate of " + apk)
    return match.group(1)


def main(source: str, out_dir: str) -> None:
    digest = cert_digest_from_apk(source) if source.endswith(".apk") else source
    text = payload(digest)
    os.makedirs(out_dir, exist_ok=True)
    with open(os.path.join(out_dir, "provisioning.json"), "w") as f:
        f.write(json.dumps(json.loads(text), indent=2) + "\n")
    try:
        import segno
    except ImportError:
        print("segno not installed: wrote provisioning.json only")
        return
    # Compact JSON keeps the QR as small as possible, which scans better.
    segno.make(text, error="m").save(os.path.join(out_dir, "provisioning-qr.png"), scale=8, border=4)
    print("wrote provisioning.json and provisioning-qr.png")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    main(sys.argv[1], sys.argv[2])
